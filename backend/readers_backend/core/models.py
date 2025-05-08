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

        # When using .latest() django will order in non increasing order by default....
        # That is, no negative sign (-) is required....
        get_latest_by = ["major_version", "minor_version", "localization"]

    def __str__(self):
        return f"{self.localization} {self.major_version}.{self.minor_version}"


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

    def __str__(self):
        return f"Workbook {self.number} of {self.collection}"


# Feedback Model
class Feedback(models.Model):
    workbook = models.ForeignKey(Workbook, on_delete=models.CASCADE)
    page_number = models.IntegerField(blank=False, null=False)
    chapter_number = models.IntegerField(blank=False, null=False)
    description = models.TextField(blank=False, null=False)
    user_email = models.EmailField(blank=False, null=False)
    created_at = models.DateTimeField(auto_now_add=True)
    major_version = models.IntegerField(blank=False, null=False)
    minor_version = models.IntegerField(blank=False, null=False)
    localization = models.CharField(max_length=5, blank=False, null=False)

    def __str__(self):
        return f"Feedback on Workbook {self.workbook.number} v{self.major_version}.{self.minor_version} - Chapter {self.chapter_number} - Page {self.page_number}"
