import json

from django.core.management import BaseCommand

from core.models import Collection
from core.serializers import CollectionCreateSerializer, WorkbookCreateSerializer
from django.core.files.base import File

CHAPTERS_DIR = "development_data/chapters"
PDFS_DIR = "development_data/pdfs"
NUM_WORKBOOKS = 36

class Command(BaseCommand):
    help = f"Seeds the database with initial data useful for development. Creates {NUM_WORKBOOKS} workbooks under collection en-US v1.0."

    def handle(self, *args, **options):

        if Collection.objects.exists():
            self.stdout.write(self.style.WARNING("Database is not empty. Skipping seeding."))
            return

        collection = {
            "localization": "en-US",
            "major_version": 1,
            "minor_version": 0,
        }

        collection_serializer = CollectionCreateSerializer(data=collection)

        if collection_serializer.is_valid():
            collection_obj = collection_serializer.save()
        else:
            self.stdout.write(self.style.ERROR("Error creating collection"))
            return

        for i in range(1, NUM_WORKBOOKS + 1):
            workbook = {
                "number": i,
                "collection": collection_obj.id,
            }

            pdf_name = f"workbook-{str(i).zfill(2)}.pdf"
            pdf_path = f"{PDFS_DIR}/{pdf_name}"

            chapters_name = f"workbook-{str(i).zfill(2)}.json"
            chapters_path = f"{CHAPTERS_DIR}/{chapters_name}"

            with open(pdf_path, "rb") as pdf, open(chapters_path, "r") as chapters_file:
                file = File(pdf, name=pdf_name)
                workbook["pdf"] = file
                workbook["chapters"] = json.load(chapters_file)

                workbook_serializer = WorkbookCreateSerializer(data=workbook)
                if workbook_serializer.is_valid():
                    workbook_serializer.save()
                    self.stdout.write(self.style.SUCCESS(f"Successfully created workbook {i}"))
                else:
                    self.stdout.write(self.style.ERROR(f"Error creating workbook {i}\n: {workbook_serializer.errors}"))
                    return

        collection_obj.is_released = True
        collection_obj.save()