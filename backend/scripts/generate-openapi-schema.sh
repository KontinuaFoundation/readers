#!/bin/bash

cd ../readers_backend

python3 manage.py spectacular --color --file schema.yml

mv schema.yml ../../docs/backend/schema.yml

cd ../../backend/scripts

echo "Schema generated and moved to docs/backend/schema.yml"

echo "To view the schema, open docs/backend/schema.yml using a openapi / swagger viewer."