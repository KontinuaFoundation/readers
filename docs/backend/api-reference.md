

## Authentication


### POST /api/token/
Retrieves token given username and password in access body.


**Example Request**

``` json
{
  "username": "Administrator",
  "password": "A-Very-Secure-Passoword"
}
 ```

**Example Response**

``` json
{
    "token": "54598435h34jhk5hkj43kjh54hkjhkj543hkj54hkj4"
}
 ```
---

### DELETE /api/token/destroy
Deletes the currently authenticated users authorization token.


### **Authentication**
This endpoint requires the inclusion of a valid **DRF TokenAuthentication** token in the `Authorization` header. This can be retrieved from POST /api/token/.


## Collections

### POST /api/collections/

Creates a new collection with a given major and minor version along with the localization.
Used to organize workbooks by version and localization

#### **Request Parameters**
1. **`major_version`** (int, required):
   - An integer representing the major version of the collection.

2. **`minor_version`** (int, required):
   - An integer representing the minor version of the collection.

3. **`localization`** (string, required):
   - The localization of the collection.

### **GET /api/collections/**
Retrieves a list of collections based on the specified filters. This endpoint is used to fetch collections organized by major and minor versions, as well as localization.

#### **Query Parameters**
1. **`major_version`** (string, optional):
   - Filters collections by the specified major version. If not provided, collections of all major versions are returned.

2. **`minor_version`** (string, optional):
   - Filters collections by the specified minor version. If not provided, collections of all minor versions are returned.

3. **`localization`** (string, optional):
   - Filters collections by the specified localization. If not provided, collections of all localizations are returned.
---

#### **Response**

- **Response Body**:
  ```json
  [
      {
        "id": 1,
        "major_version": "1",
        "minor_version": "0",
        "localization": "en-US",
        "created_at": "2023-10-01T12:00:00Z",
        "updated_at": "2023-10-01T12:00:00Z"
      },
      {
        "id": 2,
        "major_version": "1",
        "minor_version": "1",
        "localization": "fr-FR",
        "created_at": "2023-10-02T12:00:00Z",
        "updated_at": "2023-10-02T12:00:00Z"
      }
    ]


---

## Workbooks


### POST /api/workbooks/

Adds a new workbook to a given collection.

Expects collecton ID, chapter data, and pdf file for a single workbook.

### **Multipart/Form-Data Upload**

This endpoint allows uploading a workbook's metadata and PDF file together. The request must use the `multipart/form-data` format.

### **Authentication**

This endpoint requires the inclusion of a valid **DRF TokenAuthentication** token in the `Authorization` header. This can be retrieved from POST /api/token/.

Authorization: Token {your_token_here}

#### **Request Parameters**

1. **`number`** (string, required):
   - The sequence number of the workbook.

2. **`collection`** (string, required):
   - The **Primary Key (ID)** of the given collection it belongs to. Note that /api/collections/ will most likely need to be called before to get said PK.

3. **`chapters`** (string, required):
    
    - A JSON string representing the workbook's metadata.
        
    - Must include the workbook's title, identifier, and chapters information.
        
    - { "chapters": \[ { "title": "Introduction to the Kontinua Sequence", "start_page": 3, "requires": \[\], "metadata": { "description": "Introduction to Kontinua", "videos": \[\], "references": \[\] } } \]}
        
4. **`pdf`** (file, required):
    
    - The binary PDF file representing the workbook.
        
    - Example: A file named `workbook-01.pdf`.
        

## GET /api/workbooks/
Lists the latest version of all workbooks including their name, identifier, and version number.

#### **Query Parameters**
1. **`collection`** (int, required):
   - The **Primary Key (ID)** of the collection the workbooks belong to.

``` json
{
version: 1.0
workbooks: [
  {"collection": 2, "number": 1, "id": 92},
  {"collection": 2, "number": 2, "id": 93},
  {"collection": 2, "number": 3, "id": 94}
  ...
]
}

 ```

## GET /api/workbooks/{id}/

Returns all the metadata and pdf download link for a given workbook.

If no version provided, endpoint will return the latest version.

**Example response**

``` json
{
    "collection": 1,
    "number": 3,
    "pdf": "https://s3.amazonaws.com/bucket-name/workbooks/workbook-01.pdf",
    "chapters": [
        {
            "title": "Introduction to the Kontinua Sequence",
            "start_page": 3,
            "requires": [],
            "metadata": {
                "description": "Introduction to Kontinua",
                "videos": [],
                "references": []
            }
        }
    ]
}

 ```

## DELETE /api/workbooks/{id}/

Given the id of a workbook, delete it.

### **Authentication**

This endpoint requires the inclusion of a valid **DRF TokenAuthentication** token in the `Authorization` header. This can be retrieved from POST /api/token/.

Authorization: Token {your token here!}

---




