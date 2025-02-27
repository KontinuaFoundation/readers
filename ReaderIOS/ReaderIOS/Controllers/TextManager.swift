import SwiftUI

struct TextBoxData: Codable {
    var text: String
    var size: CGSize
    var position: CGPoint
}

class TextManager: ObservableObject {
    private let fileName = "textBoxes.json"

    func addText(textBoxes: Binding<[String: [TextBoxData]]>, key: String, width: CGFloat, height: CGFloat) {
        var updatedTextBoxes = textBoxes.wrappedValue
        updatedTextBoxes[key, default: []].append(
            TextBoxData(text: "", size: CGSize(width: 200, height: 100), position: CGPoint(x: width / 2, y: height / 2))
        )
        textBoxes.wrappedValue = updatedTextBoxes
    }

    func deleteText(textBoxes: Binding<[String: [TextBoxData]]>, key: String, index: Int) {
        var updatedTextBoxes = textBoxes.wrappedValue
        updatedTextBoxes[key, default: []].remove(at: index)
        textBoxes.wrappedValue = updatedTextBoxes
    }

    func deleteAllText(textBoxes: Binding<[String: [TextBoxData]]>, key: String) {
        var updatedTextBoxes = textBoxes.wrappedValue
        updatedTextBoxes[key]?.removeAll()
        textBoxes.wrappedValue = updatedTextBoxes
    }

    func bindingForTextBox(textBoxes: Binding<[String: [TextBoxData]]>, key: String,
                           index: Int) -> Binding<TextBoxData>?
    {
        guard textBoxes.wrappedValue[key] != nil, index < textBoxes.wrappedValue[key]!.count else { return nil }

        return Binding(
            get: { textBoxes.wrappedValue[key]![index] },
            set: { newValue in
                textBoxes.wrappedValue[key]![index] = newValue
            }
        )
    }

    func saveTextBoxes(textBoxes: [String: [TextBoxData]]) {
        do {
            let data = try JSONEncoder().encode(textBoxes)
            let url = getDocumentsDirectory().appendingPathComponent(fileName)
            try data.write(to: url)
            print("Text Boxes saved to \(url)")
        } catch {
            print("Error saving text boxes: \(error)")
        }
    }

    func loadTextBoxes(textBoxes: inout [String: [TextBoxData]]) {
        let url = getDocumentsDirectory().appendingPathComponent(fileName)
        guard FileManager.default.fileExists(atPath: url.path)
        else { return }
        do {
            let data = try Data(contentsOf: url)
            textBoxes = try JSONDecoder().decode([String: [TextBoxData]].self, from: data)
            print("Text Boxes loaded from \(url)")
        } catch {
            print("Error loading text boxes: \(error)")
        }
    }

    private func getDocumentsDirectory() -> URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
    }
}
