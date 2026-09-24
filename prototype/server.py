import http.server
import socketserver
import socket
import os
import webbrowser
import sys

# Ensure UTF-8 output on Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')

PORT = 8080
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DATAGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return '127.0.0.1'

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

if __name__ == '__main__':
    local_ip = get_local_ip()
    print('=' * 60)
    print('[WeParent Prototype Server]')
    print('=' * 60)
    print(f'PC Browser URL: http://localhost:{PORT}/')
    print(f'Mobile Wi-Fi URL: http://{local_ip}:{PORT}/')
    print('=' * 60)
    print('Opening default browser...')
    
    webbrowser.open(f'http://localhost:{PORT}/index.html')
    
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")
