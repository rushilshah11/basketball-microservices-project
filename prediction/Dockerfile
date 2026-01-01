# Use Python 3.11 slim image
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
# Install CPU-only PyTorch first (saves GBs of space)
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu
# Install the rest of the dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY *.py .

# Create directory for model weights
RUN mkdir -p /app/models

# Expose port
EXPOSE 5002

# Run the application
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "5002"]

