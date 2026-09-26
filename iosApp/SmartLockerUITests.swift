import XCTest

final class SmartLockerUITests: XCTestCase {
    func testStartupKeyboardAndRotationPreserveContact() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-smartlocker.environment", "DEMO"]
        app.launch()
        let phone = app.textFields["Celular"]
        XCTAssertTrue(phone.waitForExistence(timeout: 15))
        reveal(phone, in: app)
        phone.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        phone.typeText("11987654321")
        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertEqual(phone.value as? String, "11987654321")
        let landscape = XCTAttachment(screenshot: app.screenshot())
        landscape.name = "Landscape with keyboard"
        landscape.lifetime = .keepAlways
        add(landscape)
