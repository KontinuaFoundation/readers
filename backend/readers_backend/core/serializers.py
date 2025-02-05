from rest_framework import serializers

from .models import Collection, Workbook


'''
All serializers pertaining to actions performed on Workbook resource.
'''
class WorkbookCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = '__all__'

    def validate_chapters(self, value):
        # TODO: Validate the json format?
        return value

class WorkbookRetrieveSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = '__all__'

# This is only used when viewing detailed view of collection
# Should not have direct access to an endpoint.
class WorkbooksListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Workbook
        fields = ['id', 'number', 'collection']

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
        fields = ['major_version', 'minor_version', 'localization']

    def validate_major_version(self, value):
        # TODO: Must be greater than or equal to the current major version?
        return value

    def validate_minor_version(self, value):
        # TODO: Must be greater than any minor version for the given major version?
        return value

class CollectionRetrieveSerializer(serializers.ModelSerializer):
    workbooks = WorkbooksListSerializer(many=True, read_only=True)

    class Meta:
        model = Collection
        fields = '__all__'