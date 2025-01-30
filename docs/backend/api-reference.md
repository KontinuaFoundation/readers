

## Authentication


### POST /api/token/
Retrieves token given username and password in access body.

Authentication is only required for uploading new workbooks or deleting existing ones.

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

## Collections

**Coming soon!**

---

## Workbooks

### POST /api/workbooks/

Creates a new workbook at version 1.0, or creates a new workbook and autoincrements the version if a workbook with the same identifier already exists.

New versions do not overwrite old ones.

Expects metadata and pdf file for a single workbook.

### **Multipart/Form-Data Upload**

This endpoint allows uploading a workbook's metadata and PDF file together. The request must use the `multipart/form-data` format.

### **Multipart/Form-Data Upload**

This endpoint allows uploading a workbook's metadata and PDF file together. The request must use the `multipart/form-data` format.

### **Authentication**

This endpoint requires the inclusion of a valid **DRF TokenAuthentication** token in the `Authorization` header. This can be retrieved from POST /api/token/.

Authorization: Token {your_token_here}

#### **Request Parameters**

1. **`metadata`** (string, required):
    
    - A JSON string representing the workbook's metadata.
        
    - Must include the workbook's title, identifier, and chapters information.
        
    - { "title": "Workbook 01", "identifier": "workbook-01", "collection",: "chapters": \[ { "title": "Introduction to the Kontinua Sequence", "start_page": 3, "requires": \[\], "metadata": { "description": "Introduction to Kontinua", "videos": \[\], "references": \[\] } } \]}
        
2. **`file`** (file, required):
    
    - The binary PDF file representing the workbook.
        
    - Example: A file named `workbook-01.pdf`.
        

#### **Example cURL Request**

``` bash
curl -X POST "https://yourapi.com/upload_workbook" \
-H "Content-Type: multipart/form-data" \
-F 'metadata={
    "title": "Workbook 01",
    "identifier": "workbook-01",
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
}' \
-F "file=@path/to/workbook-01.pdf"

 ```
## GET /api/workbooks/
Lists the latest version of all workbooks including their name, identifier, and version number.

``` json
{
version: 1.0
workbooks: [
  {"title": "Workbook 01", "identifier": "workbook-01", },
  {"title": "Workbook 02", "identifier": "workbook-02", },
  {"title": "Workbook 03", "identifier": "workbook-03", },
  ...
]
}

 ```

## GET /api/workbooks/{identifier}?version={version}

Given a workbook identifier and an optional version number, it returns the metadata and S3 download link for the given workbook.

If no version provided, endpoint will return the latest version.

**Example response**

``` json
{
    "title": "Workbook 01",
    "identifier": "workbook-01",
    "download_link": "https://s3.amazonaws.com/bucket-name/workbooks/workbook-01.pdf",
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

## DELETE /api/workbooks/{identifier}?version={version}

Given an identifier, and a **mandatory** version number, delete the workbook.

### **Authentication**

This endpoint requires the inclusion of a valid **DRF TokenAuthentication** token in the `Authorization` header. This can be retrieved from POST /api/token/.

Authorization: Token {your token here!}

---




