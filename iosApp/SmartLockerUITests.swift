import XCTest

final class SmartLockerUITests: XCTestCase {
    func testStartupKeyboardAndRotationPreserveContact() {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        defer { XCUIDevice.shared.orientation = .portrait }
        let app = XCUIApplication()
        app.launchArguments = ["-smartlocker.environment", "DEMO"]
        app.launch()
        waitForOrientation(portrait: true, in: app)
        let phone = app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@", "Celular")).firstMatch
        XCTAssertTrue(phone.waitForExistence(timeout: 15))
        reveal(phone, in: app)
        phone.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        waitUntilUncovered(phone, in: app)
        app.typeText("11987654321")
        XCUIDevice.shared.orientation = .landscapeLeft
        waitForOrientation(portrait: false, in: app)
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        waitUntilUncovered(phone, in: app)
        XCTAssertEqual(phone.value as? String, "11987654321")
        XCTAssertGreaterThan(app.frame.width, app.frame.height)
        let landscape = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        landscape.name = "Landscape with keyboard"
        landscape.lifetime = .keepAlways
        add(landscape)
        XCUIDevice.shared.orientation = .portrait
        waitForOrientation(portrait: true, in: app)
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        waitUntilUncovered(phone, in: app)
        XCTAssertEqual(phone.value as? String, "11987654321")
        let portrait = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        portrait.name = "Portrait with keyboard"
        portrait.lifetime = .keepAlways
        add(portrait)
        app.terminate()
    }

    private func waitUntilUncovered(_ field: XCUIElement, in app: XCUIApplication) {
        let visible = NSPredicate { _, _ in
            field.frame.maxY <= app.keyboards.firstMatch.frame.minY && field.frame.height > 0
        }
        let expectation = XCTNSPredicateExpectation(predicate: visible, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [expectation], timeout: 8), .completed)
    }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<6 {
            if element.isHittable { return }
            app.swipeUp()
        }
        XCTAssertTrue(element.isHittable)
    }
}
