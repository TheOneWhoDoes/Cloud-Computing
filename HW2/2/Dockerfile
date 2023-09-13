FROM python:3.8.5-alpine
ENV PYTHONUNBUFFERED 1
ADD . .
RUN pip install -r requirements.txt
ENTRYPOINT python3 server.py
