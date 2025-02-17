from rest_framework import serializers

from .models import Collection, Workbook


'''
All serializers pertaining to actions performed on Workbook resource.
'''
class WorkbookCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = '__all__'

    def validate_chapters(self, chapters):
        REQUIRED_CHAPTER_FIELDS = ('title', 'book', 'id', 'chap_num', 'start_page')
        REQUIRED_COVER_FIELDS = ('id', 'desc')

        if not isinstance(chapters, list):
            raise serializers.ValidationError("must be a list of chapters.")

        if len(chapters) == 0:
            raise serializers.ValidationError("must have at least one chapter.")

        # For every chapter we need to have the required fields...
        for chapter in chapters:
            for field in REQUIRED_CHAPTER_FIELDS:
                if not chapter.get(field):
                    raise serializers.ValidationError({
                        field: "This field is required."
                    })

            # If this chapter has covers, we need to make sure they have the required fields...
            if "covers" in chapter:
                if not isinstance(chapter.get('covers'), list):
                    raise serializers.ValidationError({"covers": "covers must be a list of covers."})

                for cover in chapter.get('covers'):
                    for field in REQUIRED_COVER_FIELDS:
                        if not cover.get(field):
                            raise serializers.ValidationError({
                                "covers": {
                                    field: "This field is required."
                                }
                            })



        return chapters

class WorkbookRetrieveSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = '__all__'

# This is only used when viewing detailed view of collection
# Should not have direct access to an endpoint.
class WorkbooksListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = ['id', 'number']

'''
All serializers pertaining to actions performed on Collections resource.
'''
class CollectionListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Collection
        fields = '__all__'

class CollectionCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Collection
        fields = ['major_version', 'minor_version', 'localization', 'id']

    def validate(self, data):
        major_version = data.get("major_version")
        minor_version = data.get("minor_version")
        localization = data.get("localization")

        latest = Collection.objects.filter(localization=localization).order_by(
            "-major_version", "-minor_version"
        ).first()

        if not latest:
            return data

        if major_version < latest.major_version:
            raise serializers.ValidationError({
                "major_version": f"Must be at least {latest.major_version} (latest: {latest.major_version}.{latest.minor_version})."
            })

        if major_version == latest.major_version and minor_version <= latest.minor_version:
            raise serializers.ValidationError({
                "minor_version": f"Must be greater than {latest.minor_version} (latest: {latest.major_version}.{latest.minor_version})."
            })

        return data


class CollectionRetrieveSerializer(serializers.ModelSerializer):
    workbooks = WorkbooksListSerializer(many=True, read_only=True)

    class Meta:
        model = Collection
        fields = '__all__'