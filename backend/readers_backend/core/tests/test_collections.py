from django.contrib.auth.models import User
from django.urls import reverse
from rest_framework.authtoken.models import Token
from rest_framework.test import APITestCase

from core.models import Collection


class CollectionTestCase(APITestCase):

    def setUp(self):
        user = User.objects.create_user(username="testuser", password="testpass123")
        token = Token.objects.create(user=user)
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {token.key}")

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

    def test_create_collection_lower_version_than_latest(self):
        #TODO
        return

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

    def test_can_delete_collection(self):
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

    def test_list_collections(self):
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

        self.assertEqual(Collection.objects.count(), len(response.data), msg="Number of collections in response doesnt equal number of collections in DB.")

        response.data.sort(key=lambda x: x['id'])

        for index, response_collection in enumerate(response.data):
            expected = collections_data[index]

            self.assertEqual(expected["major_version"], response_collection["major_version"], msg=f'Major version mismatch')
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

        response = self.client.get(f"{url}?localization=en-US&major_version=1")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 2)