import json

from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from rest_framework.authtoken.models import Token
from rest_framework.test import APITestCase
from core.models import Collection, Workbook


class WorkbookTestCase(APITestCase):

    # A good chapters argument structure to use for testing.
    GOOD_CHAPTERS = [
        {
            "requires": [],
            "title": "Introduction to the Kontinua Sequence",
            "id": "introduction",
            "book": "01",
            "chap_num": 1,
            "start_page": 3,
            "covers": [
                {
                    "id": "kont_intro",
                    "desc": "Introduction to Kontinua",
                    "videos": [
                        {
                            "link": "https://youtu.be/example",
                            "title": "Test Video"
                        }
                    ],
                    "references": [
                        {
                            "link": "https://example.org/",
                            "title": "Test Reference"
                        }
                    ]
                }
            ],
        }
    ]

    def setUp(self):
        user = User.objects.create_user(username="testuser", password="testpass123")
        token = Token.objects.create(user=user)
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {token.key}")


    def tearDown(self):
        """Ensure uploaded files are deleted after tests"""
        for workbook in Workbook.objects.all():
            Workbook.objects.all().delete()

    def test_create_workbook(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        body = {
            'number': 1,
            'collection': collection.id,
            'chapters': json.dumps(self.GOOD_CHAPTERS),
            'pdf': pdf_file,
        }

        response = self.client.post(url, body, format='multipart')

        # Is it created?
        self.assertEqual(201, response.status_code,
                         f"Failed to create Workbook: Response status code is {response.status_code}, expected 201: {response.json()}")
        self.assertEqual(1, Workbook.objects.count(),
                         f"Workbook object was not created: Current count is {Workbook.objects.count()}, expected 1.")
        self.assertEqual(1, collection.workbooks.count(),
                         f"Workbook was not associated with the Collection: Current count is {collection.workbooks.count()}, expected 1.")

        # Does the data match what we posted?
        workbook = collection.workbooks.first()
        self.assertEqual(1, workbook.number,
                         f"Workbook number does not match expected value: Found {workbook.number}, expected 1.")
        self.assertEqual(collection, workbook.collection,
                         f"Workbook is not associated with the correct Collection: Found {workbook.collection}, expected {collection}.")
        self.assertEqual(self.GOOD_CHAPTERS, workbook.chapters,
                         f"Workbook chapters content does not match expected value: Found {workbook.chapters}, expected '{{}}'.")
        self.assertIsNotNone(workbook.pdf,
                             "Workbook PDF file was not uploaded.")

    def test_create_workbook_without_auth(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        body = {
            'number': 1,
            'collection': collection.id,
            'chapters': '{}',
            'pdf': pdf_file,
        }

        # Clear authorization token before making the request
        self.client.credentials()

        response = self.client.post(url, body, format='multipart')

        self.assertEqual(401, response.status_code,
                         f"Expected 401 Unauthorized when creating workbook without auth, got {response.status_code}.")
        self.assertEqual(0, Workbook.objects.count(),
                         f"Workbook should not be created without auth: Current count is {Workbook.objects.count()}, expected 0.")

    def test_create_workbook_with_invalid_collection(self):
        url = reverse('workbook-list')

        # Invalid collection ID
        non_existent_collection = 9999

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        body = {
            'number': 1,
            'collection': non_existent_collection,
            'chapters': '{}',
            'pdf': pdf_file,
        }

        response = self.client.post(url, body, format='multipart')

        self.assertEqual(400, response.status_code,
                         f"Expected 400 Bad Request for invalid collection ID, got {response.status_code}.")
        self.assertEqual(0, Workbook.objects.count(),
                         f"Workbook should not be created: Current count is {Workbook.objects.count()}, expected 0.")

    def test_create_workbook_with_malformed_json(self):
        url = reverse('workbook-list')

        malformed_body = '{"number": 1, "collection": 1, "chapters": {}'

        response = self.client.post(url, malformed_body, content_type="application/json")

        self.assertEqual(400, response.status_code,
                         f"Expected 400 Bad Request for malformed JSON, got {response.status_code}.")
        self.assertEqual(0, Workbook.objects.count(),
                         f"Workbook should not be created with malformed JSON: Current count is {Workbook.objects.count()}, expected 0.")

    def test_delete_workbook(self):
        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")
        workbook = Workbook.objects.create(number=1, collection=collection, chapters={}, pdf=None)
        url = reverse('workbook-detail', args=[workbook.id])

        response = self.client.delete(url)

        self.assertEqual(204, response.status_code,
                         f"Failed to delete workbook: Status code is {response.status_code}, expected 204.")
        self.assertEqual(0, Workbook.objects.count(),
                         f"Workbook should be deleted: Found {Workbook.objects.count()}, expected 0.")

    def test_delete_workbook_without_auth(self):
        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")
        workbook = Workbook.objects.create(number=1, collection=collection, chapters={}, pdf=None)
        url = reverse('workbook-detail', args=[workbook.id])

        # Clear authorization token before making the request
        self.client.credentials()

        response = self.client.delete(url)

        self.assertEqual(401, response.status_code,
                         f"Expected 401 Unauthorized when attempting to delete workbook without auth, got {response.status_code}.")
        self.assertEqual(1, Workbook.objects.count(),
                         f"Workbook should not be deleted without auth: Found {Workbook.objects.count()}, expected 1.")

    def test_list_workbooks_not_allowed(self):
        url = reverse('workbook-list')

        response = self.client.get(url)

        self.assertEqual(405, response.status_code,
                         f"Expected 405 Method Not Allowed for listing workbooks directly, got {response.status_code}.")

    def test_retrieve_workbook(self):
        # Auth shouldn't be needed here...
        self.client.credentials()

        collection = Collection.objects.create(
            major_version=1,
            minor_version=0,
            localization="en-US"
        )

        chapters_data = [
            {
                "requires": [],
                "title": "Introduction to the Kontinua Sequence",
                "id": "introduction",
                "chap_num": 1,
                "covers": [
                    {
                        "id": "kont_intro",
                        "desc": "Introduction to Kontinua",
                        "videos": [
                            {
                                "link": "https://youtu.be/example",
                                "title": "Test Video"
                            }
                        ],
                        "references": [
                            {
                                "link": "https://example.org/",
                                "title": "Test Reference"
                            }
                        ]
                    }
                ],
                "start_page": 3
            },
            {
                "requires": ["atom"],
                "title": "Atomic and Molecular Mass",
                "id": "atomic_mass",
                "chap_num": 2,
                "covers": [
                    {
                        "id": "mole",
                        "desc": "Define a mole",
                        "videos": [
                            {
                                "link": "https://youtu.be/example2",
                                "title": "Test Video 2"
                            }
                        ],
                        "references": [
                            {
                                "link": "https://example.org/mole",
                                "title": "Test Reference 2"
                            }
                        ]
                    }
                ],
                "start_page": 15
            }
        ]

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        workbook = Workbook.objects.create(
            collection=collection,
            number=1,
            pdf=pdf_file,
            chapters=chapters_data
        )

        url = reverse('workbook-detail', args=[workbook.id])

        response = self.client.get(url)

        self.assertEqual(200, response.status_code,
                         f"Failed to retrieve workbook: Status code is {response.status_code}, expected 200.")

        data = response.json()

        self.assertEqual(collection.id, data['collection'],
                         f"Collection ID mismatch: got {data['collection']}, expected {collection.id}")
        self.assertEqual(1, data['number'],
                         f"Workbook number mismatch: got {data['number']}, expected 1")
        self.assertTrue('pdf' in data,
                        "PDF field missing from response")

        self.assertTrue('chapters' in data,
                        "Chapters field missing from response")
        self.assertEqual(2, len(data['chapters']),
                         f"Wrong number of chapters: got {len(data['chapters'])}, expected 2")

        chapter1 = data['chapters'][0]
        self.assertEqual("Introduction to the Kontinua Sequence", chapter1['title'],
                         f"Chapter title mismatch: got {chapter1['title']}")
        self.assertEqual("introduction", chapter1['id'],
                         f"Chapter id mismatch: got {chapter1['id']}")
        self.assertEqual(1, chapter1['chap_num'],
                         f"Chapter number mismatch: got {chapter1['chap_num']}")
        self.assertEqual([], chapter1['requires'],
                         f"Chapter requires mismatch: got {chapter1['requires']}")
        self.assertEqual(3, chapter1['start_page'],
                         f"Chapter start_page mismatch: got {chapter1['start_page']}")

        cover1 = chapter1['covers'][0]
        self.assertEqual("kont_intro", cover1['id'],
                         f"Cover id mismatch: got {cover1['id']}")
        self.assertEqual("Introduction to Kontinua", cover1['desc'],
                         f"Cover description mismatch: got {cover1['desc']}")

        video1 = cover1['videos'][0]
        self.assertTrue('link' in video1,
                        "Video link missing from response")
        self.assertTrue('title' in video1,
                        "Video title missing from response")

        reference1 = cover1['references'][0]
        self.assertTrue('link' in reference1,
                        "Reference link missing from response")
        self.assertTrue('title' in reference1,
                        "Reference title missing from response")

        chapter2 = data['chapters'][1]
        self.assertEqual(["atom"], chapter2['requires'],
                         f"Chapter requires mismatch: got {chapter2['requires']}")

    # Workbook chapter validation tests.
    def test_create_workbook_with_no_chapters(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        body = {
            'number': 1,
            'collection': collection.id,
            'pdf': pdf_file,
        }

        response = self.client.post(url, body, format='multipart')

        self.assertEqual(400, response.status_code,
                         f"Expected 400 Bad Request for invalid chapters, got {response.status_code}.")


    def test_create_workbook_with_chapters_with_essential_fields_missing(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        essential_fields = ['title', 'book', 'id', 'chap_num', 'start_page']
        for field in essential_fields:
            chapter_data_missing_field = {
                "requires": [],
                "title": "Chapter 1",
                "book": "01",
                "id": "chapter-1",
                "chap_num": 1,
                "covers": [],
                "start_page": 1
            }
            del chapter_data_missing_field[field]

            body = {
                'number': 1,
                'collection': collection.id,
                'chapters': json.dumps([chapter_data_missing_field]),
                'pdf': pdf_file,
            }

            response = self.client.post(url, body, format='multipart')

            self.assertEqual(400, response.status_code,
                             f"Expected 400 Bad Request for missing {field}, got {response.status_code}.")
            self.assertTrue("chapters" in response.json(),
                            f"Expected 'chapters' field in response error but got {response.json()}")
            self.assertTrue(field in response.json()["chapters"],
                            f"Expected '{field}' field in chapters response error but got {response.json()['chapters']}")

    def test_create_workbook_with_chapters_with_essential_fields_empty(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        essential_fields = ['title', 'book', 'id', 'chap_num', 'start_page']
        for field in essential_fields:
            chapter_data_missing_field = [
                {
                    "requires": [],
                    "title": "Chapter 1" if field != "title" else "",
                    "book": "01" if field != "book" else "",
                    "id": "chapter-1" if field != "id" else "",
                    "chap_num": 1 if field != "chap_num" else None,
                    "covers": [],
                    "start_page": 1 if field != "start_page" else None
                }
            ]

            body = {
                'number': 1,
                'collection': collection.id,
                'chapters': json.dumps(chapter_data_missing_field),
                'pdf': pdf_file,
            }

            response = self.client.post(url, body, format='multipart')

            self.assertEqual(400, response.status_code,
                             f"Expected 400 Bad Request for missing {field}, got {response.status_code}.")
            self.assertTrue("chapters" in response.json(),
                            f"Expected 'chapters' field in response error but got {response.json()}")
            self.assertTrue(field in response.json()["chapters"],
                            f"Expected '{field}' field in chapters response error but got {response.json()['chapters']}")


    def test_create_workbook_with_chapters_with_covers_with_essential_fields_missing(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        essential_fields = ('id', 'desc')
        for field in essential_fields:

            chapter_data_missing_field = [{
                "requires": [],
                "title": "Chapter 1",
                "book": "01",
                "id": "chapter-1",
                "chap_num": 1,
                "covers": [{
                    "id": "cover-1",
                    "desc": "cover-1 desc",
                    "videos": [],
                    "references": [],
                }],
                "start_page": 1
            }]

            del chapter_data_missing_field[0]["covers"][0][field]

            response = self.client.post(url, {
                'number': 1,
                'collection': collection.id,
                'chapters': json.dumps(chapter_data_missing_field),
                'pdf': pdf_file,
            }, format='multipart')

            self.assertEqual(400, response.status_code,
                             f"Expected 400 Bad Request for missing {field}, got {response.status_code}.")
            self.assertTrue("chapters" in response.json(),
                            f"Expected 'chapters' field in response error but got {response.json()}")
            self.assertTrue("covers" in response.json()["chapters"], msg=f"Expected 'covers' field in chapters response error but got {response.json()['chapters']}")
            self.assertTrue(field in response.json()["chapters"]["covers"],
                            f"Expected '{field}' field in chapters response error but got {response.json()['chapters']}")

    def test_create_workbook_with_chapters_with_covers_with_essential_fields_empty(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        essential_fields = ('id', 'desc')
        for field in essential_fields:
            chapter_data_empty_field = [{
                "requires": [],
                "title": "Chapter 1",
                "book": "01",
                "id": "chapter-1",
                "chap_num": 1,
                "covers": [{
                    "id": "" if field == "id" else "cover-1",
                    "desc": "" if field == "desc" else "cover-1 desc",
                    "videos": [],
                    "references": [],
                }],
                "start_page": 1
            }]

            response = self.client.post(url, {
                'number': 1,
                'collection': collection.id,
                'chapters': json.dumps(chapter_data_empty_field),
                'pdf': pdf_file,
            }, format='multipart')

            self.assertEqual(400, response.status_code,
                             f"Expected 400 Bad Request for empty {field}, got {response.status_code}.")
            self.assertTrue("chapters" in response.json(),
                            f"Expected 'chapters' field in response error but got {response.json()}")
            self.assertTrue("covers" in response.json()["chapters"],
                            msg=f"Expected 'covers' field in chapters response error but got {response.json()['chapters']}")
            self.assertTrue(field in response.json()["chapters"]["covers"],
                            f"Expected '{field}' field in chapters response error but got {response.json()['chapters']}")


    def test_create_workbook_with_chapters_not_a_list(self):
        url = reverse('workbook-list')

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        body = {
            'number': 1,
            'collection': collection.id,
            'chapters': '{}',
            'pdf': pdf_file,
        }

        response = self.client.post(url, body, format='multipart')

        # Is it created?
        self.assertEqual(400, response.status_code,
                         f"Failed to create Workbook: Response status code is {response.status_code}, expected 201.")
        self.assertTrue("chapters" in response.json(),
                        f"Expected 'chapters' field in response error but got {response.json()}")

    def test_create_workbook_with_covers_not_a_list(self):
        url = reverse('workbook-list')


        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        chapter_data = [{
            "requires": [],
            "title": "Chapter 1",
            "book": "01",
            "id": "chapter-1",
            "chap_num": 1,
            "covers": {},
            "start_page": 1
        }]

        body = {
            'number': 1,
            'collection': collection.id,
            'chapters': json.dumps(chapter_data),
            'pdf': pdf_file,
        }

        response = self.client.post(url, body, format='multipart')

        self.assertEqual(400, response.status_code,
                         f"Expected 400 Bad Request, got {response.status_code}.")
        self.assertTrue("chapters" in response.json(),
                        f"Expected 'chapters' field in response error but got {response.json()}")
        self.assertTrue("covers" in response.json()["chapters"],
                        msg=f"Expected 'covers' field in chapters response error but got {response.json()['chapters']}")

