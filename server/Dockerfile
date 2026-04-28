FROM metacall/core:dev

WORKDIR /app

COPY requirements.txt .
RUN pip3 install --no-cache-dir --break-system-packages -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["python3", "app.py"]
