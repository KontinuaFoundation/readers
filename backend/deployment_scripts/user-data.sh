#!/bin/bash

# This user-data script can be used to install the backend and start the Django application on an EC2 instance.
# It assumes the public IP or elastic IP is in allowed_hosts in the Django settings.

cd ~ || { echo "Failed to enter home directory"; exit 1; }

dnf update -y
dnf install -y git python3.12 python3.12-devel

python3.12 -m venv venv
source venv/bin/activate

pip install --upgrade pip
pip install gunicorn

REPO_URL="https://github.com/KontinuaFoundation/readers.git"
TARGET_FOLDER="backend/readers_backend"

git clone --branch ec2-deploy-with-user-data --no-checkout --depth 1 --filter=blob:none "$REPO_URL" repo_temp
cd repo_temp || { echo "Failed to enter repository directory"; exit 1; }
git sparse-checkout init --cone
git sparse-checkout set "$TARGET_FOLDER"
git checkout
mv "$TARGET_FOLDER" ../
cd ..
rm -rf repo_temp
echo "Successfully cloned '$TARGET_FOLDER'"

cd readers_backend || { echo "Failed to enter readers_backend directory"; exit 1; }

pip install -r requirements.txt || { echo "Failed to install Python packages"; exit 1; }
echo "Successfully installed Python packages"

export AWS_STORAGE_BUCKET_NAME="kontinua-foundation-workbook-pdfs"
export AWS_S3_REGION_NAME="us-east-2"
export AWS_S3_ADDRESSING_STYLE="virtual"
export DJANGO_DEBUG="False"

python manage.py migrate

gunicorn readers_backend.wsgi:application --bind 0.0.0.0:80 --workers 3 &
echo "Django application started on port 80"