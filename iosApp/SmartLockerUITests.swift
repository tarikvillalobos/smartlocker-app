import XCTest

final class SmartLockerUITests: XCTestCase {
    func testStartupKeyboardAndRotationPreserveContact() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-smartlocker.environment", "DEMO"]
        app.launch()
        XCTAssertTrue(phone.waitForExistence(timeout: 15))
        reveal(phone, in: app)
        phone.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        app.typeText("11987654321")
        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertEqual(phone.value as? String, "11987654321")
        let landscape = XCTAttachment(screenshot: app.screenshot())
        landscape.name = "Landscape with keyboard"
        landscape.lifetime = .keepAlways
        add(landscape)
        XCUIDevice.shared.orientation = .portrait
        XCTAssertEqual(phone.value as? String, "11987654321")
        let portrait = XCTAttachment(screenshot: app.screenshot())
        portrait.name = "Portrait with keyboard"
        portrait.lifetime = .keepAlways
        add(portrait)
        app.terminate()
    }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<6 {
            if element.isHittable { return }
            app.swipeUp()
        }
        XCTAssertTrue(element.isHittable)
    }
}
