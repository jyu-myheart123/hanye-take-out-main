import pytest
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager
import time

BASE_URL = "http://localhost:5174"

@pytest.fixture(scope="module")
def driver():
    chrome_options = Options()
    chrome_options.add_argument("--start-maximized")
    chrome_options.add_argument("--disable-infobars")
    chrome_options.add_argument("--disable-extensions")
    
    driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=chrome_options)
    yield driver
    driver.quit()

class TestHomePage:
    def test_home_page_accessible(self, driver):
        driver.get(BASE_URL)
        time.sleep(3)
        assert "嘉园" in driver.title or "外卖" in driver.title
        print(f"页面标题: {driver.title}")

    def test_page_loaded(self, driver):
        driver.get(BASE_URL)
        time.sleep(3)
        body = driver.find_element(By.TAG_NAME, "body")
        assert body is not None
        print("页面body元素加载成功")

class TestAdminLogin:
    def test_admin_login_page(self, driver):
        driver.get(f"{BASE_URL}/#/login")
        time.sleep(3)
        print(f"登录页面标题: {driver.title}")

    def test_login_form_exists(self, driver):
        driver.get(f"{BASE_URL}/#/login")
        time.sleep(3)
        try:
            inputs = driver.find_elements(By.TAG_NAME, "input")
            print(f"找到 {len(inputs)} 个输入框")
            for i, input_elem in enumerate(inputs):
                print(f"  输入框 {i}: type={input_elem.get_attribute('type')}, placeholder={input_elem.get_attribute('placeholder')}")
        except Exception as e:
            print(f"查找输入框失败: {e}")

class TestNavigation:
    def test_navigation_links(self, driver):
        driver.get(BASE_URL)
        time.sleep(3)
        links = driver.find_elements(By.TAG_NAME, "a")
        print(f"找到 {len(links)} 个链接")
        buttons = driver.find_elements(By.TAG_NAME, "button")
        print(f"找到 {len(buttons)} 个按钮")

if __name__ == "__main__":
    pytest.main([__file__, "-v"])