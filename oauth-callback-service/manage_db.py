#!/usr/bin/env python3
"""
Deprecated shim — use `oauthctl` instead.

Maps the legacy flat verbs onto the new typer CLI so existing runbooks
(setup-plugshare.sh, DEPLOYMENT.md) keep working:

    list                   -> oauthctl app list
    show <app>             -> oauthctl app show <app>
    add ...                -> oauthctl app add ...
    update-domains ...     -> oauthctl app update-domains ...
    update-secret <app>    -> oauthctl app rotate-secret <app>
    enable|disable <app>   -> oauthctl app enable|disable <app>
    delete <app>           -> oauthctl app delete --force <app>
    sessions [app]         -> oauthctl --json token list  (informational)
"""
import sys

from app.cli import cli

_LEGACY_MAP = {
    "list": ["app", "list"],
    "show": ["app", "show"],
    "add": ["app", "add"],
    "update-domains": ["app", "update-domains"],
    "update-secret": ["app", "rotate-secret"],
    "enable": ["app", "enable"],
    "disable": ["app", "disable"],
    "delete": ["app", "delete", "--force"],
}

_LEGACY_USAGE = """manage_db.py is deprecated; use oauthctl:
  oauthctl app list|show|add|update-domains|rotate-secret|enable|disable|delete
  oauthctl user add|list|passwd|role|enable|disable|rm
  oauthctl client create|list|rotate-secret|revoke|rm
  oauthctl token mint|list|revoke
"""


def main():
    if len(sys.argv) < 2 or sys.argv[1] not in _LEGACY_MAP:
        sys.stderr.write(_LEGACY_USAGE)
        sys.exit(2)

    verb = sys.argv[1]
    args = sys.argv[2:]
    mapped = _LEGACY_MAP[verb]

    # Legacy `add` took an optional 5th positional secret; new CLI uses --secret.
    if verb == "add" and len(args) >= 5:
        args = args[:4] + ["--secret", args[4]]

    # update-secret used to accept an explicit secret; rotate-secret always
    # generates. Drop a trailing explicit secret with a warning.
    if verb == "update-secret" and len(args) > 1:
        sys.stderr.write(
            "note: explicit secrets are no longer accepted; generating a new one\n"
        )
        args = args[:1]

    # delete used to prompt; the legacy script deleted unconditionally
    sys.argv = ["oauthctl"] + mapped + args
    cli()


if __name__ == "__main__":
    sys.stderr.write(
        "note: manage_db.py is deprecated; use `oauthctl app ...` instead\n"
    )
    main()
