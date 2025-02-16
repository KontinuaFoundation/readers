# **Kontinua Readers Backend**

---

The readers backend allows Kontinua administrators to upload workbooks and their corresponding metadata. Addtionally, Kontinua clients such as the readers app can retrieve workbooks and their metadata.

## **Getting started guide**

Coming soon!

## Authentication

The backend only requires authentication for users who wish to upload/delete workbooks and their metadata.

There should be no publically accessible endpoint for creating a user. User creation should be part of pre-deployment as the only adminstrator as of now is Aaron.

For login we will use Django REST Frameworks TokenAuthentication as it will work best with the CLI interface that will be used for uploads.

This token will be retrieved from POST /api/token/ where username and password are passed to the endpoint.

The CLI will then store the token in memory and include it in subsequent upload requests.

## Rate and usage limits

TODO!

This will need to be implemented as there will be several public endpoints.