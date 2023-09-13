import json
import socket
from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO
import requests
import os


class ServerRequestHandler(BaseHTTPRequestHandler):

    def get_weather_condition(self):
        url = open('env', 'r').readline().strip().replace('URL=', '')
        try:
            res = requests.get(url=url)
            data = res.json()['current']
            return {
                       'temperature': data['temperature'],
                       'weather descriptions': data['weather_descriptions'],
                       'wind speed': data['wind_speed'],
                       'humidity': data['humidity'],
                       'feelslike': data['feelslike']
                   }, 200
        except Exception as e:
            print(e)
            return {'message': 'An error occurred'}, 500

    def do_GET(self):
        hostname = socket.gethostname()
        weather, status = self.get_weather_condition()

        response = {
            'hostname': hostname,
            **weather
        }
        self.send_message(response, status)

    def do_POST(self):
        pass

    def send_message(self, message, status_code):
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        response = BytesIO()
        message = json.dumps({'message': message})
        message = message.encode('utf-8')
        response.write(message)
        self.wfile.write(response.getvalue())


def main():
    server = None
    try:
        port = int(open('env', 'r').readlines()[1].strip().replace('PORT=', ''))
        server = HTTPServer(('0.0.0.0', port), ServerRequestHandler)
        print(f'server is listening on port {port} ...')
        server.serve_forever()
    except KeyboardInterrupt:
        print('^C received, shutting down server')
    finally:
        if server:
            server.socket.close()


if __name__ == '__main__':
    main()
