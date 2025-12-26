FROM python:3.11-slim

WORKDIR /app

# Install system dependencies (gcc is often needed for some python libraries)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# 1. Copy requirements first
COPY requirements.txt .

# 2. Install ALL dependencies from the file
RUN pip install --no-cache-dir -r requirements.txt

# 3. Copy application code
COPY app.py .

# Run the application
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "5001"]