#!/bin/bash

# This script will run the django backend with some pre-populated data.
# It will also run the server on the local ipv4 address of the user so
# it can be accessed by other devices on the same network.

# Some constants...
IP=$(ipconfig getifaddr en0)
bold=$(tput bold)
green=$(tput setaf 2)
reset=$(tput sgr0)

# 1.) Ensure python3.12 is installed
if ! command -v python3.12 &> /dev/null
then
    echo "Error: python3.12 could not be found."
    echo "Please install Python 3.12  (Homebrew is nice):"
    echo "  brew install python@3.12"
    echo "For more information, visit:"
    echo "  https://docs.brew.sh/Installation"
    exit 1
fi

# 2.) Create a virtual environment if it doesn't exist and activate it.
cd ../readers_backend/ || exit 2
if [ ! -d "venv" ]; then
    python3.12 -m venv venv
fi
source venv/bin/activate

# 3.) Install requirements.txt in the virtual environment.
pip install -r requirements.txt

# Set the environment variables for Django.
export DJANGO_DEBUG=True

# 4.) Run any database migrations
python3 manage.py migrate

# 5.) Populate the database with workbooks and pdfs.
python3 manage.py populate_db

# 6.) Start the Django server using the local ipv4 address of the user.

echo ""
echo "============================================"
echo "  🚀 ${bold}How To Connect${reset} 🚀  "
echo "============================================"
echo ""
echo "🔗 Connect to the server by visiting: ${bold}${green}http://$IP:8000${reset} in your browser or by changing the base url in the mobile app."
echo "⚠️ This script uses your devices local ipv4 address to run the server. It may change if you switch networks."
echo ""
echo "============================================"
echo ""

python3 manage.py runserver $IP:8000