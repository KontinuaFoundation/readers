from django.db import models


class Collection(models.Model):
    major_version = models.IntegerField(blank=False, null=False)
    minor_version = models.IntegerField(blank=False, null=False)
    localization = models.TextField(blank=False, null=False, max_length=5)
    is_released = models.BooleanField(default=False)
    creation_date = models.DateTimeField(auto_now_add=True, blank=False, null=False)

    class Meta:
        unique_together = ("major_version", "minor_version", "localization")
        ordering = ["-major_version", "-minor_version", "localization"]


# Create your models here.
class Workbook(models.Model):
    number = models.IntegerField(blank=False, null=False)
    collection = models.ForeignKey(
        Collection, on_delete=models.CASCADE, related_name="workbooks"
    )
    chapters = models.JSONField(blank=False, null=False)
    pdf = models.FileField(blank=False, null=False)

    class Meta:
        unique_together = ("number", "collection")
        ordering = ["number", "collection"]


# Feedback Model
class Feedback(models.Model):
    workbook = models.ForeignKey(Workbook, on_delete=models.CASCADE)
    page_number = models.IntegerField(blank=False, null=False, default=0)
    chapter_number = models.IntegerField(blank=False, null=False, default=0)
    description = models.TextField(blank=False, null=False, default="")
    user_email = models.EmailField(blank=False, null=False, default="")
    created_at = models.DateTimeField(auto_now_add=True, default="")
    major_version = models.IntegerField(blank=False, null=False, default=1)
    minor_version = models.IntegerField(blank=False, null=False, default=0)
    localization = models.CharField(
        max_length=5, blank=False, null=False, default="en-US"
    )

    def __str__(self):
        return f"Feedback on Workbook {self.workbook.number} v{self.major_version}.{self.minor_version} - Chapter {self.chapter_number} - Page {self.page_number}"
