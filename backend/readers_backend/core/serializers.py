import jsonschema
from jsonschema import validate
from rest_framework import serializers
from .models import Collection, Workbook, Feedback
import logging

logger = logging.getLogger(__name__)

"""
All serializers pertaining to actions performed on Workbook resource.
"""


class WorkbookCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = "__all__"

    def validate_chapters(self, data):
        schema = {
            "type": "array",
            "minItems": 1,
            "items": {
                "type": "object",
                "required": ["title", "id", "chap_num", "start_page", "covers"],
                "properties": {
                    "requires": {"type": "array", "items": {"type": "string"}},
                    "title": {"type": "string", "minLength": 1},
                    "id": {"type": "string", "minLength": 1},
                    "chap_num": {"type": "integer"},
                    "start_page": {"type": "integer"},
                    "covers": {
                        "type": "array",
                        "minItems": 0,
                        "items": {
                            "type": "object",
                            "required": ["id", "desc"],
                            "properties": {
                                "id": {"type": "string", "minLength": 1},
                                "desc": {"type": "string", "minLength": 1},
                                "videos": {
                                    "type": "array",
                                    "items": {
                                        "type": "object",
                                        "required": ["link", "title"],
                                        "properties": {
                                            "link": {"type": "string", "minLength": 1},
                                            "title": {"type": "string", "minLength": 1},
                                        },
                                        "additionalProperties": False,
                                    },
                                },
                                "references": {
                                    "type": "array",
                                    "items": {
                                        "type": "object",
                                        "required": ["link", "title"],
                                        "properties": {
                                            "link": {"type": "string", "minLength": 1},
                                            "title": {"type": "string", "minLength": 1},
                                        },
                                        "additionalProperties": False,
                                    },
                                },
                            },
                            "additionalProperties": False,
                        },
                    },
                },
                "additionalProperties": False,
            },
        }

        try:
            validate(instance=data, schema=schema)
        except jsonschema.exceptions.ValidationError as e:
            raise serializers.ValidationError(str(e))

        return data


class WorkbookRetrieveSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = "__all__"


# This is only used when viewing detailed view of collection
# Should not have direct access to an endpoint.
class WorkbooksListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = ["id", "number"]


"""
All serializers pertaining to actions performed on Collections resource.
"""


class CollectionListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Collection
        fields = "__all__"


class CollectionCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Collection
        fields = ["major_version", "minor_version", "localization", "id"]

    def validate(self, data):
        major_version = data.get("major_version")
        minor_version = data.get("minor_version")
        localization = data.get("localization")

        latest = (
            Collection.objects.filter(localization=localization)
            .order_by("-major_version", "-minor_version")
            .first()
        )

        if not latest:
            return data

        if major_version < latest.major_version:
            raise serializers.ValidationError(
                {
                    "major_version": f"Must be at least {latest.major_version} (latest: {latest.major_version}.{latest.minor_version})."
                }
            )

        if (
            major_version == latest.major_version
            and minor_version <= latest.minor_version
        ):
            raise serializers.ValidationError(
                {
                    "minor_version": f"Must be greater than {latest.minor_version} (latest: {latest.major_version}.{latest.minor_version})."
                }
            )

        return data


class CollectionRetrieveSerializer(serializers.ModelSerializer):
    workbooks = WorkbooksListSerializer(many=True, read_only=True)

    class Meta:
        model = Collection
        fields = "__all__"


# For validation query params when retrieving a collection.
class CollectionRetrieveQueryParamsSerializer(serializers.Serializer):
    major_version = serializers.IntegerField(required=False)
    minor_version = serializers.IntegerField(required=False)
    localization = serializers.CharField(required=False)
    is_released = serializers.BooleanField(
        required=False, default=None, allow_null=True
    )


class FeedbackSerializer(serializers.ModelSerializer):

    # Validate the fields in the feedback object
    class Meta:
        model = Feedback
        fields = [
            "workbook_id",
            "chapter_number",
            "page_number",
            "user_email",
            "description",
            "major_version",
            "minor_version",
            "localization",
        ]

    # Validate that the workbook_id exists
    workbook_id = serializers.PrimaryKeyRelatedField(
        source="workbook",
        queryset=Workbook.objects.all(),
        error_messages={
            "does_not_exist": "Workbook with this ID does not exist",
            "required": "Workbook ID is required",
        },
    )

    # Validate that the version (major_version, minor_version, localization) exists
    def validate(self, data):
        """
        Validate that the version (major_version, minor_version, localization) exists
        for the associated workbook's collection.
        """
        workbook = data.get("workbook")
        major_version = data.get("major_version")
        minor_version = data.get("minor_version")
        localization = data.get("localization")

        # If any of the fields are missing, return the data as is
        if not all(
            [
                workbook,
                major_version is not None,
                minor_version is not None,
                localization,
            ]
        ):
            return data

        # Verify the version exists
        version_exists = Collection.objects.filter(
            major_version=major_version,
            minor_version=minor_version,
            localization=localization,
        ).exists()

        # If the version does not exist, raise a validation error
        if not version_exists:
            logger.debug("DEBUG: Raising validation error")
            raise serializers.ValidationError(
                {
                    "version": f"Version {major_version}.{minor_version} ({localization}) does not exist."
                }
            )

        return data
