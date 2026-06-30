"""
嘉园外卖Web端UI自动化测试脚本
使用Selenium4 + POM架构编写

运行方式：
1. 安装依赖：pip install selenium pytest allure-pytest webdriver-manager
2. 下载浏览器驱动：自动下载（使用webdriver-manager）
3. 运行测试：pytest test_ui.py --alluredir=allure-results
4. 生成报告：allure generate allure-results -o allure-report

测试环境：
- 浏览器：Chrome 125+
- 测试地址：http://localhost:8080
"""

import pytest
import allure
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from selenium.webdriver.chrome.options import Options
import time

class BasePage:
    """基础页面类，封装通用方法"""
    
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, 10)
    
    def find_element(self, locator):
        """查找元素"""
        return self.wait.until(EC.presence_of_element_located(locator))
    
    def click(self, locator):
        """点击元素"""
        element = self.find_element(locator)
        element.click()
    
    def send_keys(self, locator, text):
        """输入文本"""
        element = self.find_element(locator)
        element.clear()
        element.send_keys(text)
    
    def get_text(self, locator):
        """获取元素文本"""
        return self.find_element(locator).text
    
    def is_displayed(self, locator):
        """判断元素是否显示"""
        try:
            return self.find_element(locator).is_displayed()
        except:
            return False
    
    def wait_for_element(self, locator):
        """等待元素出现"""
        self.wait.until(EC.visibility_of_element_located(locator))
    
    def wait_for_clickable(self, locator):
        """等待元素可点击"""
        self.wait.until(EC.element_to_be_clickable(locator))


class LoginPage(BasePage):
    """登录页面"""
    
    # 元素定位器
    PHONE_INPUT = (By.ID, "phone")
    CODE_INPUT = (By.ID, "code")
    LOGIN_BUTTON = (By.ID, "loginBtn")
    SEND_CODE_BUTTON = (By.ID, "sendCodeBtn")
    ERROR_MESSAGE = (By.ID, "errorMsg")
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = "http://localhost:8080/login"
    
    def open(self):
        """打开登录页面"""
        self.driver.get(self.url)
    
    def enter_phone(self, phone):
        """输入手机号"""
        self.send_keys(self.PHONE_INPUT, phone)
    
    def enter_code(self, code):
        """输入验证码"""
        self.send_keys(self.CODE_INPUT, code)
    
    def click_login(self):
        """点击登录按钮"""
        self.click(self.LOGIN_BUTTON)
    
    def click_send_code(self):
        """点击发送验证码"""
        self.click(self.SEND_CODE_BUTTON)
    
    def get_error_message(self):
        """获取错误信息"""
        return self.get_text(self.ERROR_MESSAGE)


class HomePage(BasePage):
    """首页"""
    
    # 元素定位器
    SHOP_LIST = (By.CLASS_NAME, "shop-item")
    SEARCH_INPUT = (By.ID, "searchInput")
    SEARCH_BUTTON = (By.ID, "searchBtn")
    CART_ICON = (By.ID, "cartIcon")
    USER_ICON = (By.ID, "userIcon")
    
    def __init__(self, driver):
        super().__init__(driver)
    
    def is_shop_list_displayed(self):
        """判断店铺列表是否显示"""
        return self.is_displayed(self.SHOP_LIST)
    
    def search_shop(self, keyword):
        """搜索店铺"""
        self.send_keys(self.SEARCH_INPUT, keyword)
        self.click(self.SEARCH_BUTTON)
    
    def click_first_shop(self):
        """点击第一个店铺"""
        self.click(self.SHOP_LIST)


class ShopDetailPage(BasePage):
    """店铺详情页"""
    
    # 元素定位器
    SHOP_NAME = (By.ID, "shopName")
    DISH_LIST = (By.CLASS_NAME, "dish-item")
    ADD_TO_CART_BTN = (By.CLASS_NAME, "add-cart-btn")
    CART_COUNT = (By.ID, "cartCount")
    
    def __init__(self, driver):
        super().__init__(driver)
    
    def get_shop_name(self):
        """获取店铺名称"""
        return self.get_text(self.SHOP_NAME)
    
    def add_dish_to_cart(self, index=0):
        """添加菜品到购物车"""
        dishes = self.driver.find_elements(*self.DISH_LIST)
        if dishes:
            add_btn = dishes[index].find_element(*self.ADD_TO_CART_BTN)
            add_btn.click()
    
    def get_cart_count(self):
        """获取购物车数量"""
        return self.get_text(self.CART_COUNT)


class CartPage(BasePage):
    """购物车页面"""
    
    # 元素定位器
    CART_ITEM = (By.CLASS_NAME, "cart-item")
    CLEAR_CART_BTN = (By.ID, "clearCartBtn")
    CHECKOUT_BTN = (By.ID, "checkoutBtn")
    EMPTY_CART_MSG = (By.ID, "emptyCartMsg")
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = "http://localhost:8080/cart"
    
    def open(self):
        """打开购物车页面"""
        self.driver.get(self.url)
    
    def is_cart_empty(self):
        """判断购物车是否为空"""
        return self.is_displayed(self.EMPTY_CART_MSG)
    
    def click_checkout(self):
        """点击结算按钮"""
        self.click(self.CHECKOUT_BTN)
    
    def click_clear_cart(self):
        """点击清空购物车"""
        self.click(self.CLEAR_CART_BTN)


class OrderPage(BasePage):
    """订单页面"""
    
    # 元素定位器
    ORDER_LIST = (By.CLASS_NAME, "order-item")
    ORDER_STATUS = (By.CLASS_NAME, "order-status")
    SUBMIT_ORDER_BTN = (By.ID, "submitOrderBtn")
    
    def __init__(self, driver):
        super().__init__(driver)
    
    def click_submit_order(self):
        """点击提交订单"""
        self.click(self.SUBMIT_ORDER_BTN)
    
    def get_order_status(self):
        """获取订单状态"""
        return self.get_text(self.ORDER_STATUS)


@pytest.fixture(scope="module")
def driver():
    """初始化浏览器驱动"""
    # 配置Chrome选项
    chrome_options = Options()
    chrome_options.add_argument("--start-maximized")
    chrome_options.add_argument("--disable-infobars")
    chrome_options.add_argument("--disable-extensions")
    
    # 使用webdriver-manager自动管理驱动
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=chrome_options)
    
    yield driver
    
    # 清理：关闭浏览器
    driver.quit()


class TestLogin:
    """登录功能测试"""
    
    @allure.feature("用户登录")
    @allure.story("正确登录")
    def test_login_success(self, driver):
        """测试正确登录"""
        login_page = LoginPage(driver)
        
        with allure.step("打开登录页面"):
            login_page.open()
        
        with allure.step("输入手机号"):
            login_page.enter_phone("13800138000")
        
        with allure.step("输入验证码"):
            login_page.enter_code("123456")
        
        with allure.step("点击登录"):
            login_page.click_login()
        
        with allure.step("验证登录成功"):
            home_page = HomePage(driver)
            assert home_page.is_shop_list_displayed()
    
    @allure.feature("用户登录")
    @allure.story("验证码错误")
    def test_login_wrong_code(self, driver):
        """测试验证码错误"""
        login_page = LoginPage(driver)
        
        with allure.step("打开登录页面"):
            login_page.open()
        
        with allure.step("输入手机号"):
            login_page.enter_phone("13800138000")
        
        with allure.step("输入错误验证码"):
            login_page.enter_code("654321")
        
        with allure.step("点击登录"):
            login_page.click_login()
        
        with allure.step("验证错误提示"):
            assert "验证码错误" in login_page.get_error_message()


class TestShop:
    """店铺浏览测试"""
    
    @allure.feature("店铺浏览")
    @allure.story("查看店铺列表")
    def test_view_shop_list(self, driver):
        """测试查看店铺列表"""
        home_page = HomePage(driver)
        
        with allure.step("打开首页"):
            driver.get("http://localhost:8080")
        
        with allure.step("验证店铺列表显示"):
            assert home_page.is_shop_list_displayed()
    
    @allure.feature("店铺浏览")
    @allure.story("搜索店铺")
    def test_search_shop(self, driver):
        """测试搜索店铺"""
        home_page = HomePage(driver)
        
        with allure.step("打开首页"):
            driver.get("http://localhost:8080")
        
        with allure.step("搜索店铺"):
            home_page.search_shop("麻辣烫")
        
        with allure.step("验证搜索结果"):
            assert home_page.is_shop_list_displayed()


class TestCart:
    """购物车功能测试"""
    
    @allure.feature("购物车")
    @allure.story("添加商品到购物车")
    def test_add_to_cart(self, driver):
        """测试添加商品到购物车"""
        home_page = HomePage(driver)
        shop_detail_page = ShopDetailPage(driver)
        
        with allure.step("打开首页"):
            driver.get("http://localhost:8080")
        
        with allure.step("进入店铺"):
            home_page.click_first_shop()
        
        with allure.step("添加菜品到购物车"):
            shop_detail_page.add_dish_to_cart()
        
        with allure.step("验证购物车数量"):
            cart_count = shop_detail_page.get_cart_count()
            assert int(cart_count) >= 1
    
    @allure.feature("购物车")
    @allure.story("清空购物车")
    def test_clear_cart(self, driver):
        """测试清空购物车"""
        cart_page = CartPage(driver)
        
        with allure.step("打开购物车页面"):
            cart_page.open()
        
        with allure.step("清空购物车"):
            cart_page.click_clear_cart()
        
        with allure.step("验证购物车为空"):
            assert cart_page.is_cart_empty()


class TestOrder:
    """订单功能测试"""
    
    @allure.feature("订单")
    @allure.story("提交订单")
    def test_submit_order(self, driver):
        """测试提交订单"""
        cart_page = CartPage(driver)
        order_page = OrderPage(driver)
        
        with allure.step("打开购物车"):
            cart_page.open()
        
        with allure.step("点击结算"):
            cart_page.click_checkout()
        
        with allure.step("提交订单"):
            order_page.click_submit_order()
        
        with allure.step("验证订单提交成功"):
            assert "待支付" in order_page.get_order_status()


if __name__ == "__main__":
    pytest.main([
        __file__,
        "--alluredir=allure-results",
        "-v",
        "--tb=short"
    ])