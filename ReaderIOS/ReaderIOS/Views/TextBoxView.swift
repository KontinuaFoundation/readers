import SwiftUI

struct TextView: View {
    @ObservedObject var textManager: TextManager
    @Binding var textBoxes: [String: [TextBoxData]]
    var key: String
    @Binding var deleteTextBox: Bool
    @Binding var currentTextBoxIndex: Int
    var width: CGFloat
    var height: CGFloat
    var moveEnabled: Bool

    var body: some View {
        ZStack {
            if let keys = textBoxes[key]?.indices {
                ForEach(keys, id: \.self) { index in
                    // Pass a binding to each TextBoxData element
                    if let binding = textManager.bindingForTextBox(textBoxes: $textBoxes, key: key, index: index) {
                        TextBox(
                            data: binding,
                            key: key,
                            index: index,
                            deleteTextBox: $deleteTextBox,
                            currentTextBoxIndex: $currentTextBoxIndex,
                            textBoxes: $textBoxes,
                            textManager: textManager,
                            width: width,
                            height: height,
                            moveEnabled: moveEnabled
                        )
                    }
                }
            }
        }
    }
}

struct TextBox: View {
    @Binding var data: TextBoxData
    var key: String
    var index: Int
    @Binding var deleteTextBox: Bool
    @Binding var currentTextBoxIndex: Int
    @Binding var textBoxes: [String: [TextBoxData]]
    @ObservedObject var textManager: TextManager
    var width: CGFloat
    var height: CGFloat
    var moveEnabled: Bool

    var body: some View {
        ZStack {
            TextEditor(text: $data.text)
                .frame(width: data.size.width, height: data.size.height)
                .border(Color.black, width: 1)
                .background(Color(.white))
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            if moveEnabled {
                                let newX = data.position.x + value.location.x
                                let newY = data.position.y + value.location.y
                                let clampedX = min(max(newX, 0), width - 100)
                                let clampedY = min(max(newY, 0 + 50), height - 50)
                                data.position = CGPoint(x: clampedX, y: clampedY)
                            } else {
                                let newWidth = max(min(data.size.width + (value.translation.width * 0.005), 400), 100)
                                let newHeight = max(min(data.size.height + (value.translation.height * 0.005), 200), 50)
                                data.size = CGSize(width: newWidth, height: newHeight)
                            }
                        }
                        .onEnded { _ in
                            textManager.saveTextBoxes(textBoxes: textBoxes)
                        }
                )
                .onTapGesture(count: 2) {
                    deleteTextBox = true
                    currentTextBoxIndex = index
                }
                .onChange(of: data.text) { _, _ in
                    textManager.saveTextBoxes(textBoxes: textBoxes)
                }
                .position(x: data.position.x, y: data.position.y)
        }
    }
}
