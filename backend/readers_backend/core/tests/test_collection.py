from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from rest_framework.authtoken.models import Token
from rest_framework.test import APITestCase

from core.models import Collection, Workbook


class CollectionTestCase(APITestCase):

    def setUp(self):
        user = User.objects.create_user(username="testuser", password="testpass123")
        token = Token.objects.create(user=user)
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {token.key}")

    def tearDown(self):
        """Ensure uploaded files are deleted after tests"""
        for workbook in Workbook.objects.all():
            Workbook.objects.all().delete()

    def test_create_collection(self):
        url = reverse('collection-list')

        body = {
            "major_version": 1,
            "minor_version": 0,
            "localization": "en-US"
        }

        response = self.client.post(url, body)

        self.assertEqual(
            response.status_code, 201,
            msg=f"Expected status code 201, but got {response.status_code}. Response data: {response.data}"
        )

        self.assertTrue(
            Collection.objects.exists(),
            msg="No Collection object was created in the database."
        )

        collection = Collection.objects.first()

        self.assertEqual(
            collection.major_version, 1,
            msg=f"Expected major_version to be 1, but got {collection.major_version}."
        )

        self.assertEqual(
            collection.minor_version, 0,
            msg=f"Expected minor_version to be 0, but got {collection.minor_version}."
        )

        self.assertEqual(
            collection.localization, "en-US",
            msg=f"Expected localization to be 'en-US', but got {collection.localization}."
        )

        self.assertFalse(
            collection.is_released,
            msg="Expected is_released to be False, but it was True."
        )

        self.assertIsNotNone(
            collection.creation_date,
            msg="Expected creation_date to be set, but it was None."
        )

    def test_create_collection_without_auth(self):
        url = reverse('collection-list')

        body = {
            "major_version": 1,
            "minor_version": 0,
            "localization": "en-US"
        }

        # Clear authorization token before making the request
        self.client.credentials()

        response = self.client.post(url, body)

        self.assertEqual(
            response.status_code, 401,
            msg=f"Expected status code 401 Unauthorized, but got {response.status_code}."
        )
        self.assertEqual(
            Collection.objects.count(), 0,
            msg="Collection should not have been created without authorization."
        )

    def test_cant_create_duplicate_major_minor_local(self):
        body = {
            "major_version": 1,
            "minor_version": 0,
            "localization": "en-US"
        }

        collection = Collection.objects.create(**body)

        url = reverse('collection-list')

        response = self.client.post(url, body)
        self.assertEqual(response.status_code, 400, msg=f"Expected status code 400, but got {response.status_code}.")

        self.assertTrue('non_field_errors' in response.data, msg='Error response does not contain non_field_errors.')

        self.assertEqual("The fields major_version, minor_version, localization must make a unique set.",
                         response.data['non_field_errors'][0])

    def test_create_same_locale_different_version(self):
        url = reverse('collection-list')

        body = {
            "major_version": 1,
            "minor_version": 0,
            "localization": "en-US"
        }

        collection = Collection.objects.create(**body)

        body['minor_version'] = 1

        response = self.client.post(url, body)

        self.assertEqual(
            response.status_code, 201,
            msg=f"Expected status code 201, but got {response.status_code}. Response data: {response.data}"
        )

        self.assertTrue(
            Collection.objects.exists(),
            msg="No Collection object was created in the database."
        )

    def test_create_same_version_different_locale(self):
        url = reverse('collection-list')

        body = {
            "major_version": 1,
            "minor_version": 0,
            "localization": "en-US"
        }

        collection = Collection.objects.create(**body)

        body['localization'] = 'en-CC'

        response = self.client.post(url, body)

        self.assertEqual(
            response.status_code, 201,
            msg=f"Expected status code 201, but got {response.status_code}. Response data: {response.data}"
        )

        self.assertTrue(
            Collection.objects.exists(),
            msg="No Collection object was created in the database."
        )

    def test_delete_collection(self):
        # TODO: Test deleting collection also deletes the workbook.

        url = reverse('collection-detail', kwargs={'pk': 1})

        body = {
            "major_version": 1,
            "minor_version": 0,
            "localization": "en-US"
        }

        collection = Collection.objects.create(**body)

        response = self.client.delete(url)

        self.assertEqual(response.status_code, 204, msg=f"Expected status code 204, but got {response.status_code}.")
        self.assertEqual(0, Collection.objects.count())

    def test_delete_collection_without_auth(self):
        url = reverse('collection-detail', kwargs={'pk': 1})

        body = {
            "major_version": 1,
            "minor_version": 0,
            "localization": "en-US"
        }

        collection = Collection.objects.create(**body)

        # Clear authorization token before making the request
        self.client.credentials()

        response = self.client.delete(url)

        self.assertEqual(
            response.status_code, 401,
            msg=f"Expected status code 401 Unauthorized, but got {response.status_code}."
        )
        self.assertEqual(
            Collection.objects.count(), 1,
            msg="Collection should not have been deleted without authorization."
        )

    def test_list_collections(self):
        # auth shouldn't be needed here
        self.client.credentials()

        url = reverse('collection-list')

        collections_data = [
            {
                "major_version": 1,
                "minor_version": 0,
                "localization": "en-US",
                "is_released": False,
            },
            {
                "major_version": 1,
                "minor_version": 1,
                "localization": "fr-FR",
                "is_released": False,
            },
            {
                "major_version": 1,
                "minor_version": 2,
                "localization": "fr-FR",
                "is_released": False,
            },
        ]

        for data in collections_data:
            Collection.objects.create(**data)

        response = self.client.get(url)
        self.assertEqual(response.status_code, 200, msg=f"Expected status code 200, but got {response.status_code}.")

        self.assertEqual(Collection.objects.count(), len(response.data),
                         msg="Number of collections in response doesnt equal number of collections in DB.")

        response.data.sort(key=lambda x: x['id'])

        for index, response_collection in enumerate(response.data):
            expected = collections_data[index]

            self.assertEqual(expected["major_version"], response_collection["major_version"],
                             msg=f'Major version mismatch')
            self.assertEqual(expected["minor_version"], response_collection["minor_version"])
            self.assertEqual(expected["localization"], response_collection["localization"])
            self.assertEqual(expected["is_released"], response_collection["is_released"])

    def test_list_collections_filter_major_version(self):
        collections_data = [
            {"major_version": 1, "minor_version": 0, "localization": "en-US"},
            {"major_version": 2, "minor_version": 0, "localization": "en-US"},
            {"major_version": 3, "minor_version": 0, "localization": "en-US"},
        ]

        for data in collections_data:
            Collection.objects.create(**data)

        url = reverse('collection-list')
        response = self.client.get(f"{url}?major_version=2")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['major_version'], 2)

    def test_list_collections_filter_minor_version(self):
        collections_data = [
            {"major_version": 1, "minor_version": 0, "localization": "en-US"},
            {"major_version": 1, "minor_version": 1, "localization": "en-US"},
            {"major_version": 1, "minor_version": 2, "localization": "en-US"},
        ]

        for data in collections_data:
            Collection.objects.create(**data)

        url = reverse('collection-list')
        response = self.client.get(f"{url}?minor_version=1")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['minor_version'], 1)

    def test_list_collections_filter_localization(self):
        collections_data = [
            {"major_version": 1, "minor_version": 0, "localization": "en-US"},
            {"major_version": 1, "minor_version": 0, "localization": "fr-FR"},
            {"major_version": 1, "minor_version": 0, "localization": "es-ES"},
        ]

        for data in collections_data:
            Collection.objects.create(**data)

        url = reverse('collection-list')
        response = self.client.get(f"{url}?localization=fr-FR")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['localization'], "fr-FR")

    def test_list_collections_filter_is_released(self):
        collections_data = [
            {"major_version": 1, "minor_version": 0, "localization": "en-US", "is_released": True},
            {"major_version": 2, "minor_version": 0, "localization": "en-US", "is_released": False},
        ]

        for data in collections_data:
            Collection.objects.create(**data)

        url = reverse('collection-list')

        response = self.client.get(f"{url}?is_released=true")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)
        self.assertTrue(response.data[0]['is_released'])

        response = self.client.get(f"{url}?is_released=false")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)
        self.assertFalse(response.data[0]['is_released'])

    def test_list_collections_multiple_filters(self):
        collections_data = [
            {"major_version": 1, "minor_version": 0, "localization": "en-US", "is_released": True},
            {"major_version": 1, "minor_version": 0, "localization": "fr-FR", "is_released": True},
            {"major_version": 2, "minor_version": 0, "localization": "en-US", "is_released": False},
        ]

        for data in collections_data:
            Collection.objects.create(**data)

        url = reverse('collection-list')
        response = self.client.get(f"{url}?major_version=1&localization=en-US&is_released=true")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['major_version'], 1)
        self.assertEqual(response.data[0]['localization'], "en-US")
        self.assertTrue(response.data[0]['is_released'])

    def test_list_collection_fetches_sorted_by_latest_by_default(self):
        collections_data = [
            {"major_version": 1, "minor_version": 0, "localization": "en-US"},
            {"major_version": 1, "minor_version": 1, "localization": "en-US"},
            {"major_version": 2, "minor_version": 0, "localization": "en-US"},
            {"major_version": 2, "minor_version": 1, "localization": "en-US"},
            {"major_version": 1, "minor_version": 0, "localization": "fr-FR"},
            {"major_version": 1, "minor_version": 1, "localization": "fr-FR"},
        ]

        for data in collections_data:
            Collection.objects.create(**data)

        url = reverse('collection-list')

        response = self.client.get(f"{url}?localization=en-US")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 4)
        self.assertEqual(response.data[0]['major_version'], 2)
        self.assertEqual(response.data[0]['minor_version'], 1)
        self.assertEqual(response.data[0]['localization'], "en-US")


    def test_retrieve_collection(self):

        # auth shouldn't be required here.
        self.client.credentials()

        collection = Collection.objects.create(major_version=1, minor_version=0, localization="en-US")

        # Create a test PDF file
        pdf_content = b'%PDF-1.4 fake pdf content'
        pdf_file = SimpleUploadedFile(
            name='test.pdf',
            content=pdf_content,
            content_type='application/pdf'
        )

        for i in range(3):
            Workbook.objects.create(number=i + 1, collection=collection, chapters={}, pdf=pdf_file)

        url = reverse('collection-detail', kwargs={'pk': collection.id})

        response = self.client.get(url)

        self.assertEqual(response.status_code, 200, msg=f"Expected status code 200, but got {response.status_code}.")

        # General collection info.
        self.assertEqual(response.data['id'], collection.id,
                         msg=f"Expected collection ID to be {collection.id}, but got {response.data['id']}.")
        self.assertEqual(response.data['major_version'], collection.major_version,
                         msg=f"Expected major_version to be {collection.major_version}, but got {response.data['major_version']}.")
        self.assertEqual(response.data['minor_version'], collection.minor_version,
                         msg=f"Expected minor_version to be {collection.minor_version}, but got {response.data['minor_version']}.")
        self.assertEqual(response.data['localization'], collection.localization,
                         msg=f"Expected localization to be {collection.localization}, but got {response.data['localization']}.")
        self.assertEqual(response.data['is_released'], collection.is_released,
                         msg=f"Expected is_released to be {collection.is_released}, but got {response.data['is_released']}.")
        self.assertIsNotNone(response.data['creation_date'], msg="Expected creation_date to be set, but got None.")

        # Workbooks should now also be included.

        self.assertTrue(
            'workbooks' in response.data,
            msg="Response does not contain 'workbooks' key.")

        self.assertEqual(len(response.data['workbooks']), 3,
                         msg=f"Expected 3 workbooks in response, but got {len(response.data['workbooks'])}.")

        for i in range(3):
            workbook_response = response.data['workbooks'][i]
            self.assertEqual(i + 1, workbook_response['number'],
                             msg=f"Expected workbook number to be {i + 1}, but got {workbook_response['number']}.")


    def test_retrieve_collection_not_found(self):
        url = reverse('collection-detail', kwargs={'pk': 9999})  # Non-existing collection ID

        response = self.client.get(url)

        self.assertEqual(
            response.status_code, 404,
            msg=f"Expected status code 404 Not Found, but got {response.status_code}."
        )