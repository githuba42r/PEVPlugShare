"""
HTML templates for OAuth callback pages
"""


def create_redirect_page(
    title: str,
    message: str,
    redirect_url: str,
    is_error: bool = False
) -> str:
    """Create HTML redirect page"""
    status_color = "#dc3545" if is_error else "#28a745"
    icon = "❌" if is_error else "✅"

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="2;url={redirect_url}">
    <title>{title}</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }}
        .container {{
            background: white;
            padding: 3rem;
            border-radius: 1rem;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            text-align: center;
            max-width: 500px;
        }}
        .icon {{
            font-size: 4rem;
            margin-bottom: 1rem;
        }}
        h1 {{
            color: {status_color};
            margin: 0 0 1rem 0;
            font-size: 1.75rem;
        }}
        p {{
            color: #666;
            margin: 1rem 0;
            line-height: 1.6;
        }}
        .redirect-notice {{
            margin-top: 2rem;
            padding: 1rem;
            background: #f8f9fa;
            border-radius: 0.5rem;
            font-size: 0.875rem;
            color: #666;
        }}
        a {{
            color: #667eea;
            text-decoration: none;
            font-weight: 500;
        }}
        a:hover {{
            text-decoration: underline;
        }}
        .spinner {{
            margin: 2rem auto;
            width: 40px;
            height: 40px;
            border: 4px solid #f3f3f3;
            border-top: 4px solid #667eea;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }}
        @keyframes spin {{
            0% {{ transform: rotate(0deg); }}
            100% {{ transform: rotate(360deg); }}
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">{icon}</div>
        <h1>{title}</h1>
        <p>{message}</p>
        <div class="spinner"></div>
        <div class="redirect-notice">
            <p>Redirecting back to PEV PlugShare...</p>
            <p>If you are not redirected, <a href="{redirect_url}">click here</a>.</p>
        </div>
    </div>
</body>
</html>"""


def create_error_page(title: str, message: str, hint: str) -> str:
    """Create HTML error page"""
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title}</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }}
        .container {{
            background: white;
            padding: 3rem;
            border-radius: 1rem;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            text-align: center;
            max-width: 500px;
        }}
        .icon {{
            font-size: 4rem;
            margin-bottom: 1rem;
        }}
        h1 {{
            color: #dc3545;
            margin: 0 0 1rem 0;
            font-size: 1.75rem;
        }}
        p {{
            color: #666;
            margin: 1rem 0;
            line-height: 1.6;
        }}
        .hint {{
            margin-top: 2rem;
            padding: 1rem;
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            border-radius: 0.25rem;
            text-align: left;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">⚠️</div>
        <h1>{title}</h1>
        <p>{message}</p>
        <div class="hint">
            <strong>💡 Tip:</strong> {hint}
        </div>
    </div>
</body>
</html>"""
