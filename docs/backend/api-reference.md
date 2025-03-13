# API Reference

## Authentication

All authenticated endpoints require a valid **DRF TokenAuthentication** token in the `Authorization` header.

### Token Management

#### Get Token

- **POST** `/api/token/`
- **Description**: Obtains authentication token
- **Request Body**:

```json
{
  "username": "string",
  "password": "string"
}
```

- **Response**:

```json
{
  "token": "string"
}
```

#### Delete Token

- **DELETE** `/api/token/destroy`
- **Description**: Invalidates current user's token
- **Authentication**: Required

## Collections

### Collection Operations

#### Create Collection

- **POST** `/api/collections/`
- **Description**: Creates a new collection
- **Authentication**: Required
- **Request Body**:

```json
{
  "major_version": "integer",
  "minor_version": "integer",
  "localization": "string"
}
```

#### List Collections

- **GET** `/api/collections/`
- **Description**: Retrieves filtered collections list
- **Query Parameters**:
  - `major_version` (integer, optional)
  - `minor_version` (integer, optional)
  - `localization` (string, optional)
  - `is_released` (boolean, optional)
- **Notes**:
  - Invalid parameters are ignored
  - Results ordered by major_version DESC, minor_version DESC
- **Response**:

```json
[
  {
    "id": "integer",
    "major_version": "integer",
    "minor_version": "integer",
    "localization": "string",
    "created_at": "datetime",
    "is_released": "boolean"
  }
]
```

#### Get Collection

- **GET** `/api/collections/{id}`
- **Description**: Retrieves a single collection with workbooks
- **Response**:

```json
{
  "id": "integer",
  "major_version": "integer",
  "minor_version": "integer",
  "localization": "string",
  "created_at": "datetime",
  "is_released": "boolean",
  "workbooks": [
    {
      "collection": "integer",
      "number": "integer",
      "id": "integer"
    }
  ]
}
```

#### Delete Collection

- **DELETE** `/api/collections/{id}`
- **Description**: Deletes collection and associated workbooks
- **Authentication**: Required

## Workbooks

### Workbook Operations

#### Create Workbook

- **POST** `/api/workbooks/`
- **Description**: Creates workbook in collection
- **Authentication**: Required
- **Content-Type**: multipart/form-data
- **Request Parameters**:
  - `number` (string, required)
  - `collection` (integer, required): Collection ID
  - `chapters` (JSON string, required): Workbook metadata
  - `pdf` (file, required): Workbook PDF file

#### Get Workbook Details

- **GET** `/api/workbooks/{id}`
- **Description**: Retrieves workbook metadata and PDF link
- **Response**:

```json
{
  "collection": "integer",
  "number": "integer",
  "pdf": "string",
  "chapters": [
    {
      "requires (optional)": "array of strings",
      "title": "string",
      "book": "string",
      "id": "string",
      "chap_num": "integer",
      "start_page": "integer",
      "covers": [
        {
          "id": "string",
          "desc": "string",
          "videos (optional)": [
            {
              "link": "string",
              "title": "string"
            }
          ],
          "references (optional)": [
            {
              "link": "string",
              "title": "string"
            }
          ]
        }
      ]
    }
  ]
}
```

#### Delete Workbook

- **DELETE** `/api/workbooks/{id}`
- **Description**: Removes workbook
- **Authentication**: Required

#### Send Email

- **POST** `/api/feedback/`
- **Description**: Sends an email to Kontinua email account
- **ENV**: Create an .env file and add the following
  - EMAIL_HOST=smtp.emailprovider.com #Whatever email host you use
  - EMAIL_PORT="your port" #defaults to 587
  - EMAIL_USE_TLS=True/False #TLS is recommended for security reasons
  - EMAIL_HOST_USER=your.email@gmail.com
  - EMAIL_HOST_PASSWORD=your-app-password
- **Authentication**: Not required
- **Content-Type**: application/json
- **Request Parameters**:
- `workbook_id` (Integer, Required): Workbook number
- `chapter_number` (Integer, Required)
- `page_number` (Integer, Required)
- `email` (string, required): User email
- `description` (string, required): User submitted feedback
- `major_version` (Integer, Required)
- `minor_version` (Integer, Required)
- `localization` (string, required)
