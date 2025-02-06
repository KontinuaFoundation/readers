import SwiftUI

struct TextView: View {
    @ObservedObject var textManager: TextManager
    @Binding var textBoxes: [String: [TextBoxData]]
    var key: String
    @Binding var deleteTextBox: Bool
    @Binding var currentTextBoxIndex: Int
    var width: CGFloat
    var height: CGFloat
    @Binding var textOpened: Bool

    var body: some View {
        ZStack {
            if let keys = textBoxes[key]?.indices {
                ForEach(keys, id: \.self) { index in
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
                            textOpened: $textOpened
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
    @Binding var textOpened: Bool
    @FocusState private var isFocused: Bool

    var body: some View {
        ZStack {
            VStack {
                TextEditor(text: $data.text)
                    .frame(width: data.size.width, height: data.size.height)
                    .border(isFocused ? Color.blue : Color.black, width: 3)
                    .scrollContentBackground(.hidden)
                    .foregroundStyle(Color.black)
                    .cornerRadius(8)
                    .focused($isFocused)
                    .onChange(of: data.text) {
                        textManager.saveTextBoxes(textBoxes: textBoxes)
                    }
                    .onChange(of: isFocused) {
                        if isFocused {
                            textOpened = isFocused
                        }
                    }
                    .onChange(of: textOpened) {
                        if !textOpened {
                            isFocused = textOpened
                        }
                    }
                    .onAppear {
                        if textOpened {
                            isFocused = true
                        } else {
                            isFocused = false
                        }
                    }
                    .onTapGesture(count: 2) {
                        deleteTextBox = true
                        currentTextBoxIndex = index
                    }
            }
            .position(x: data.position.x, y: data.position.y)
            Rectangle()
                .frame(width: 20, height: 20)
                .foregroundColor(.gray)
                .cornerRadius(3)
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            let newWidth = max(
                                min(data.size.width + value.translation.width, 400),
                                100
                            )
                            let newHeight = max(
                                min(data.size.height + value.translation.height, 200),
                                50
                            )
                            data.size = CGSize(width: newWidth, height: newHeight)
                        }
                        .onEnded { _ in
                            textManager.saveTextBoxes(textBoxes: textBoxes)
                        }
                )
                .position(x: data.position.x + 100 * (data.size.width / 200),
                          y: data.position.y + 50 * (data.size.height / 100))
            Rectangle()
                .frame(width: 30, height: 20)
                .foregroundColor(.gray)
                .cornerRadius(3)
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            let newX = data.position.x + value.location.x
                            let newY = data.position.y + value.location.y
                            let clampedX = min(max(newX, 0), width - 100)
                            let clampedY = min(max(newY, 0 + 50 + data.size.height), height - 50)
                            data.position = CGPoint(x: clampedX, y: clampedY)
                        }
                        .onEnded { _ in
                            textManager.saveTextBoxes(textBoxes: textBoxes)
                        }
                )
                .position(x: data.position.x, y: data.position.y - 50 * (data.size.height / 100))
        }
    }
}
