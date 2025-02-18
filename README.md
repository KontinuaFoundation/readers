readers
Source code for the e-reader apps

### About the e-reader

Kontinua is an app in Swift Native, for iOS deployment. It will be used on iPads to help children get a STEM education they may not otherwise have available to them.

### Start PDF Static Server For Development

1. Ensure python3 is installed on your device
2. Navigate to `readers/`
3. Run `./serve_pdf.sh`
4. PDFs and PDF metadata are now served on `localhost:8000`!
5. In Xcode, preview should open up and you can navigate around the app using your mouse

### Running the Project

In XCode you can use the "preview" view, or the "simulator" view. Once you have the server up and running, both views will work and show you the current status of the app.

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


### Documentation

Documentation for both the backend and frontend client can be found in the [docs directory](docs/).