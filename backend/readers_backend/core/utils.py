from django.core.mail import EmailMessage
from django.conf import settings
from django.template.loader import render_to_string
import json



def send_feedback_email(feedback):
    """
    Send feedback from a user to the app's own email address.

    Args:
        feedback: Feedback object with all required information

    Returns:
        bool: Whether the email was sent successfully
    """
    # Use version info directly from the feedback object
    subject = f"User Feedback: Workbook {feedback.workbook.number} (v{feedback.major_version}.{feedback.minor_version} {feedback.localization})"

    # Prepare context for the email
    context = {
        "feedback": feedback,
        "workbook_number": feedback.workbook.number,
        "major_version": feedback.major_version,
        "minor_version": feedback.minor_version,
        "localization": feedback.localization,
        "page_number": feedback.page_number,
        "chapter_number": feedback.chapter_number,
        "description": feedback.description,
        "user_email": feedback.user_email,
        "date_submitted": feedback.created_at.strftime("%Y-%m-%d %H:%M:%S"),
    }

    # Handle logs - iOS sends JSON as a string, Django stores it as JSON
    if feedback.logs:
        try:
            # If logs is a string (from iOS), parse it
            if isinstance(feedback.logs, str):
                logs_data = json.loads(feedback.logs)
            else:
                # If it's already a dict/list (stored as JSONField)
                logs_data = feedback.logs

            # Ensure all expected nested properties exist
            if isinstance(logs_data, dict):
                # Check device properties
                if "device" in logs_data and isinstance(logs_data["device"], dict):
                    device = logs_data["device"]
                    # Ensure all device properties exist
                    device.setdefault("model", "Unknown")
                    device.setdefault("systemName", "Unknown")
                    device.setdefault("systemVersion", "Unknown")
                    device.setdefault("appVersion", "Unknown")
                    device.setdefault("buildNumber", "Unknown")
                    device.setdefault("deviceName", "Unknown")

                # Check summary properties
                if "summary" in logs_data and isinstance(logs_data["summary"], dict):
                    summary = logs_data["summary"]
                    summary.setdefault("totalLogs", 0)
                    summary.setdefault("errorCount", 0)
                    summary.setdefault("warningCount", 0)
                    summary.setdefault("criticalCount", 0)
                    summary.setdefault("timeRange", "N/A")

                context["logs"] = logs_data
            else:
                # If logs is not a dict, wrap it
                context["logs"] = {"raw": str(logs_data)}

        except json.JSONDecodeError:
            # If parsing fails, treat as plain text
            context["logs"] = {"raw": str(feedback.logs)}
        except Exception as e:
            # Catch any other errors
            print(f"Error processing logs: {e}")
            context["logs"] = {"raw": str(feedback.logs)}

    # Create email body
    html_message = render_to_string("emails/feedback.html", context)

    # Create email
    email = EmailMessage(
        subject=subject,
        body=html_message,
        from_email=settings.DEFAULT_FROM_EMAIL,
        to=[settings.FEEDBACK_EMAIL],
    )

    email.content_subtype = "html"  # Main content is now HTML

    try:
        email.send()
        return True
    except Exception as e:
        print(f"Error sending feedback email: {e}")
        return False
