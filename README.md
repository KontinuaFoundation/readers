### Molly Sandler, Ethan Handelman, Devin Hadley, Luis Rodriguez, Jonas Schiessel | September 2024 - June 2025

### About the e-reader

Readers is an educational platform that combines a backend API with iOS and Android mobile applications. The platform delivers the Kontinua Workbook Sequence, making STEM education accessible to children who might not have access to these learning resources through traditional means.

### Starting the Development Backend

To run the development server that serves PDFs and metadata:

1. Prerequisites:
   - Install Docker or [Docker Desktop](https://docs.docker.com/get-started/introduction/get-docker-desktop/)
   - Install Python 3.12

2. Start the server:
   - Open a terminal
   - Navigate to `backend/scripts`
   - Run `./start-backend-macos.sh`

3. The server will start on your local machine:
   - Access via your local IPv4 address
   - Default port: 8000
   - Serves PDFs and metadata

4. Configure your app:
   - Update the backend API URL in your app's constants file
   - This connects your app to the development server

### Running the Project

In XCode you can use the "preview" view, or the "simulator" view. Once you have the server up and running, both views will work and show you the current status of the app.
In Android Studio you can use the "preview" view on the right hand side of the IDE.
Please ensure you choose a tablet for both versions!!

### SwiftLint

SwiftLint is a tool that helps you write clean code. It is run automatically when you build the project in XCode, and any errors will be displayed in the console.
It is also run automatically in the GitHub Actions workflow, and any errors will be displayed in the console.
Swiftlint is configured in the `.swiftlint.yml` file where some defualt rules are set and might need to be modified.
There is no need to download SwiftLint, as it is automatically installed in Xcode as a Swift Package Dependency.
To run SwiftLint, simply build the project in XCode, and the SwiftLint will run automatically, any errors will be displayed in the console.

### SwiftFormat

SwiftFormat is a tool that helps you format your code. It is run automatically in the GitHub Actions workflow, and any errors will be displayed in the console.
SwiftFormat is configured in the `.swiftformat` file where some defualt rules are set and might need to be modified.
There is no need to download SwiftFormat, as it is automatically installed in Xcode as a Swift Package Dependency.
To run SwiftFormat, In Xcode right click ReaderIOS root folder and select "SwiftFormatPlugin" from the context menu. This will open a new window with options on Directories and/ or files to format. Skip the test directories for now unless you want to format them.

### Android Format & Lint

Ktlint is a tool that helps you format and lint your code. It is run automatically in the GitHub Actions workflow, and any errors will be displayed in the console.
Ktlint is configured in the `.editorConfig` file where some defualt rules are set and might need to be modified.
There is no need to download ktlint, as it is managed by Gradle, however you can install the android studio plugin for editor formatting.
To run the Formatter, In the terminal, navigate to the `ReadersAndroidJetpack` folder and run `./gradlew ktlintFormat`, to check for linting errors or formatting errors run `./gradlew ktlintCheck`.

### Documentation

API documentation for the backend is automatically generated with DRF-Spectacular and can be found in `docs/backend`.

To regenerate the documentation you will need to run `./backend/scripts/generate-openapi-schema.sh`

To view it, use a swagger documentation editor like [this](https://editor.swagger.io/) or install a viewer in you code editor.


### Features Available
PDF Display

Annotations (pen, highlight, text, erase, clear)

Pomodoro timer (15, 20, 25, custom minutes)

Feedback submissions

Workbook + chapter navigation

Sequence wide search functionality 

Page bookmarking for future reading

Workbook and page persistence

Additional per chapter digital resources

Local workbook caching

Workbook updates via CLI tool 


### Future Enhancements
Phone and computer compatibility 

Stylus compatibility 

Split view landscape mode

Enhanced local caching

Enhanced annotations 

Enhanced feedback submissions + user data


