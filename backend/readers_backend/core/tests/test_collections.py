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
