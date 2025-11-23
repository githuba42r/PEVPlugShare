#!/usr/bin/env python3
"""
Database management utility for OAuth Callback Service
Provides CLI for managing applications without requiring sqlite3 command
"""
import sqlite3
import sys
import secrets
from datetime import datetime


DB_PATH = "/app/data/oauth_sessions.db"


def list_applications(conn):
    """List all applications"""
    cursor = conn.cursor()
    cursor.execute("""
        SELECT app_id, name, allowed_domains, deep_link_scheme, is_active, updated_at
        FROM applications
        ORDER BY app_id
    """)

    apps = cursor.fetchall()
    if not apps:
        print("No applications found.")
        return

    print("\n" + "="*100)
    print(f"{'App ID':<20} {'Name':<25} {'Active':<8} {'Domains':<45}")
    print("="*100)

    for app_id, name, domains, scheme, is_active, updated in apps:
        active_str = "✓ Yes" if is_active else "✗ No"
        # Truncate long domain lists
        domain_display = domains if len(domains) <= 42 else domains[:39] + "..."
        print(f"{app_id:<20} {name:<25} {active_str:<8} {domain_display:<45}")

    print("="*100 + "\n")


def show_application(conn, app_id):
    """Show detailed information for an application"""
    cursor = conn.cursor()
    cursor.execute("""
        SELECT app_id, name, allowed_domains, deep_link_scheme, shared_secret, is_active, created_at, updated_at
        FROM applications
        WHERE app_id = ?
    """, (app_id,))

    app = cursor.fetchone()
    if not app:
        print(f"Application '{app_id}' not found.")
        return

    app_id, name, domains, scheme, secret, is_active, created, updated = app

    print("\n" + "="*80)
    print(f"Application: {name}")
    print("="*80)
    print(f"App ID:           {app_id}")
    print(f"Name:             {name}")
    print(f"Active:           {'Yes ✓' if is_active else 'No ✗'}")
    print(f"Allowed Domains:  {domains}")
    print(f"Deep Link Scheme: {scheme}")
    print(f"Shared Secret:    {secret[:20]}... (truncated)")
    print(f"Created:          {created}")
    print(f"Updated:          {updated}")
    print("="*80 + "\n")


def update_domains(conn, app_id, domains):
    """Update allowed domains for an application"""
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE applications
        SET allowed_domains = ?, updated_at = ?
        WHERE app_id = ?
    """, (domains, datetime.now().isoformat(), app_id))

    if cursor.rowcount > 0:
        conn.commit()
        print(f"✓ Updated domains for '{app_id}' to: {domains}")
    else:
        print(f"✗ Application '{app_id}' not found.")


def update_secret(conn, app_id, secret=None):
    """Update shared secret for an application"""
    if secret is None:
        secret = secrets.token_urlsafe(32)
        print(f"Generated new secret: {secret}")

    cursor = conn.cursor()
    cursor.execute("""
        UPDATE applications
        SET shared_secret = ?, updated_at = ?
        WHERE app_id = ?
    """, (secret, datetime.now().isoformat(), app_id))

    if cursor.rowcount > 0:
        conn.commit()
        print(f"✓ Updated secret for '{app_id}'")
        print(f"⚠️  Remember to update your mobile app with the new secret!")
    else:
        print(f"✗ Application '{app_id}' not found.")


def toggle_active(conn, app_id, active):
    """Enable or disable an application"""
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE applications
        SET is_active = ?, updated_at = ?
        WHERE app_id = ?
    """, (1 if active else 0, datetime.now().isoformat(), app_id))

    if cursor.rowcount > 0:
        conn.commit()
        status = "enabled" if active else "disabled"
        print(f"✓ Application '{app_id}' {status}")
    else:
        print(f"✗ Application '{app_id}' not found.")


def add_application(conn, app_id, name, domains, scheme, secret=None):
    """Add a new application"""
    if secret is None:
        secret = secrets.token_urlsafe(32)
        print(f"Generated secret: {secret}")

    now = datetime.now().isoformat()
    cursor = conn.cursor()

    try:
        cursor.execute("""
            INSERT INTO applications (app_id, name, allowed_domains, deep_link_scheme, shared_secret, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 1, ?, ?)
        """, (app_id, name, domains, scheme, secret, now, now))

        conn.commit()
        print(f"✓ Added application '{app_id}'")
        print(f"  Secret: {secret}")
        print(f"⚠️  Save the secret - you'll need it in your mobile app!")
    except sqlite3.IntegrityError:
        print(f"✗ Application '{app_id}' already exists.")


def delete_application(conn, app_id, force=False):
    """Delete an application"""
    if not force:
        response = input(f"Are you sure you want to delete '{app_id}'? (yes/no): ")
        if response.lower() != 'yes':
            print("Deletion cancelled.")
            return

    cursor = conn.cursor()
    cursor.execute("DELETE FROM applications WHERE app_id = ?", (app_id,))

    if cursor.rowcount > 0:
        conn.commit()
        print(f"✓ Deleted application '{app_id}'")
    else:
        print(f"✗ Application '{app_id}' not found.")


def show_sessions(conn, app_id=None):
    """Show active sessions"""
    cursor = conn.cursor()

    if app_id:
        cursor.execute("""
            SELECT session_id, app_id, provider, created_at, expires_at, used
            FROM oauth_sessions
            WHERE app_id = ?
            ORDER BY created_at DESC
            LIMIT 50
        """, (app_id,))
    else:
        cursor.execute("""
            SELECT session_id, app_id, provider, created_at, expires_at, used
            FROM oauth_sessions
            ORDER BY created_at DESC
            LIMIT 50
        """)

    sessions = cursor.fetchall()
    if not sessions:
        print("No sessions found.")
        return

    print("\n" + "="*120)
    print(f"{'Session ID':<40} {'App':<15} {'Provider':<12} {'Created':<20} {'Used':<6}")
    print("="*120)

    for session_id, app, provider, created, expires, used in sessions:
        used_str = "✓ Yes" if used else "✗ No"
        print(f"{session_id:<40} {app:<15} {provider:<12} {created:<20} {used_str:<6}")

    print("="*120 + "\n")


def print_usage():
    """Print usage information"""
    print("""
OAuth Callback Service - Database Manager

Usage: python manage_db.py <command> [arguments]

Commands:
  list                              List all applications
  show <app_id>                     Show detailed info for an application
  add <app_id> <name> <domains> <scheme> [secret]
                                    Add a new application
  update-domains <app_id> <domains> Update allowed domains
  update-secret <app_id> [secret]   Update shared secret (generates if not provided)
  enable <app_id>                   Enable an application
  disable <app_id>                  Disable an application
  delete <app_id>                   Delete an application
  sessions [app_id]                 Show recent sessions (optionally filtered by app)

Examples:
  python manage_db.py list
  python manage_db.py show pevplugshare
  python manage_db.py add myapp "My App" "myapp.com,dev.myapp.com" "myapp://oauth/callback"
  python manage_db.py update-domains pevplugshare "newdomain.com,localhost:8000"
  python manage_db.py update-secret pevplugshare
  python manage_db.py disable myapp
  python manage_db.py sessions pevplugshare
""")


def main():
    if len(sys.argv) < 2:
        print_usage()
        sys.exit(1)

    command = sys.argv[1].lower()

    # Connect to database
    try:
        conn = sqlite3.connect(DB_PATH)
    except sqlite3.Error as e:
        print(f"✗ Database error: {e}")
        sys.exit(1)

    try:
        if command == "list":
            list_applications(conn)

        elif command == "show":
            if len(sys.argv) < 3:
                print("Usage: python manage_db.py show <app_id>")
                sys.exit(1)
            show_application(conn, sys.argv[2])

        elif command == "add":
            if len(sys.argv) < 6:
                print("Usage: python manage_db.py add <app_id> <name> <domains> <scheme> [secret]")
                sys.exit(1)
            secret = sys.argv[6] if len(sys.argv) > 6 else None
            add_application(conn, sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], secret)

        elif command == "update-domains":
            if len(sys.argv) < 4:
                print("Usage: python manage_db.py update-domains <app_id> <domains>")
                sys.exit(1)
            update_domains(conn, sys.argv[2], sys.argv[3])

        elif command == "update-secret":
            if len(sys.argv) < 3:
                print("Usage: python manage_db.py update-secret <app_id> [secret]")
                sys.exit(1)
            secret = sys.argv[3] if len(sys.argv) > 3 else None
            update_secret(conn, sys.argv[2], secret)

        elif command == "enable":
            if len(sys.argv) < 3:
                print("Usage: python manage_db.py enable <app_id>")
                sys.exit(1)
            toggle_active(conn, sys.argv[2], True)

        elif command == "disable":
            if len(sys.argv) < 3:
                print("Usage: python manage_db.py disable <app_id>")
                sys.exit(1)
            toggle_active(conn, sys.argv[2], False)

        elif command == "delete":
            if len(sys.argv) < 3:
                print("Usage: python manage_db.py delete <app_id>")
                sys.exit(1)
            delete_application(conn, sys.argv[2])

        elif command == "sessions":
            app_id = sys.argv[2] if len(sys.argv) > 2 else None
            show_sessions(conn, app_id)

        else:
            print(f"Unknown command: {command}")
            print_usage()
            sys.exit(1)

    finally:
        conn.close()


if __name__ == "__main__":
    main()
