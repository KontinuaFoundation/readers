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
      "title": "string",
      "start_page": "integer",
      "requires": "array",
      "metadata": {
        "description": "string",
        "videos": "array",
        "references": "array"
      }
    }
  ]
}
```

#### Delete Workbook

- **DELETE** `/api/workbooks/{id}`
- **Description**: Removes workbook
- **Authentication**: Required