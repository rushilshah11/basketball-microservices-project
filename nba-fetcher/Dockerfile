FROM python:3.11-slim

WORKDIR /app

# Install dependencies
# If you have requirements.txt, copy it. If not, we install manually for now.
# COPY requirements.txt .
# RUN pip install --no-cache-dir -r requirements.txt
RUN pip install fastapi uvicorn nba_api requests

# Copy your source code
COPY app.py .

# Run the application
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "5000"]