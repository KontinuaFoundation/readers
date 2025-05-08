# Register your models here.

from django.contrib import admin
from .models import Collection, Feedback, Workbook

admin.site.site_header = "Kontinua Readers Management"
admin.site.site_title = "Kontinua Readers Management"
admin.site.index_title = "Kontinua Readers Management"

@admin.register(Collection)
class CollectionAdmin(admin.ModelAdmin):
    ordering = (
        "-major_version",
        "-minor_version",
        "localization",
    )
    list_filter    = (
        "is_released",
        "localization",
    )

@admin.register(Workbook)
class WorkbookAdmin(admin.ModelAdmin):
    list_filter = (
        "collection",          
        "number",
    )
    ordering = (
        "collection__major_version",
        "collection__minor_version",
        "collection__localization",
        "number",
    )

@admin.register(Feedback)
class FeedbackAdmin(admin.ModelAdmin):
    list_display = ("workbook", "page_number", "chapter_number", "user_email", "created_at")
    list_filter = (
        "workbook__collection",
        "major_version", 
        "minor_version",
        "localization",
        "created_at",
    )