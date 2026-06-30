"""
嘉园外卖接口自动化测试脚本
使用Pytest框架编写，支持Allure报告生成

运行方式：
1. 安装依赖：pip install pytest requests allure-pytest pyyaml
2. 运行测试：pytest test_api.py --alluredir=allure-results
3. 生成报告：allure generate allure-results -o allure-report
4. 查看报告：allure serve allure-report

测试环境：
- 测试服务器地址：http://192.168.1.100:8081
- 数据库：MySQL 8.0.35
- Redis：7.2.4
"""

import pytest
import requests
import yaml
import json
import allure
from datetime import datetime

# 读取测试配置
with open('config.yaml', 'r', encoding='utf-8') as f:
    config = yaml.safe_load(f)

BASE_URL = config['base_url']
HEADERS = {"Content-Type": "application/json"}

class TestUserAPI:
    """用户模块接口测试"""
    
    @allure.feature("用户登录")
    @allure.story("正确手机号登录")
    @pytest.mark.parametrize("phone, code, expected_code", [
        ("13800138000", "123456", 0),      # 正确登录
        ("13800138001", "123456", 1),      # 未注册用户
        ("13800138000", "654321", 1),      # 错误验证码
        ("", "123456", 1),                 # 空手机号
    ])
    def test_user_login(self, phone, code, expected_code):
        """测试用户登录接口"""
        with allure.step(f"请求登录接口，手机号：{phone}，验证码：{code}"):
            data = {"phone": phone, "code": code}
            response = requests.post(
                f"{BASE_URL}/user/user/login",
                headers=HEADERS,
                data=json.dumps(data)
            )
        
        with allure.step("验证响应状态码"):
            assert response.status_code == 200
        
        with allure.step("验证业务响应码"):
            result = response.json()
            assert result["code"] == expected_code

    @allure.feature("用户信息")
    @allure.story("获取用户信息")
    def test_get_user_info(self):
        """测试获取用户信息接口"""
        with allure.step("请求获取用户信息"):
            response = requests.get(f"{BASE_URL}/user/user/info", headers=HEADERS)
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0

    @allure.feature("发送验证码")
    @allure.story("发送验证码成功")
    def test_send_code(self):
        """测试发送验证码接口"""
        with allure.step("请求发送验证码"):
            response = requests.get(
                f"{BASE_URL}/user/user/sendCode",
                params={"phone": "13800138000"}
            )
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0


class TestShopAPI:
    """店铺模块接口测试"""
    
    @allure.feature("店铺状态")
    @allure.story("获取店铺状态")
    def test_get_shop_status(self):
        """测试获取店铺状态接口"""
        with allure.step("请求获取店铺状态"):
            response = requests.get(f"{BASE_URL}/user/shop/status")
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0
            assert "data" in result

    @allure.feature("分类列表")
    @allure.story("获取分类列表")
    def test_get_category_list(self):
        """测试获取分类列表接口"""
        with allure.step("请求获取分类列表"):
            response = requests.get(f"{BASE_URL}/user/category/list")
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0
            assert isinstance(result["data"], list)


class TestDishAPI:
    """菜品模块接口测试"""
    
    @allure.feature("菜品列表")
    @allure.story("获取菜品列表")
    def test_get_dish_list(self):
        """测试获取菜品列表接口"""
        with allure.step("请求获取菜品列表"):
            response = requests.get(
                f"{BASE_URL}/user/dish/list",
                params={"categoryId": 1}
            )
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0

    @allure.feature("菜品详情")
    @allure.story("获取菜品详情")
    def test_get_dish_detail(self):
        """测试获取菜品详情接口"""
        with allure.step("请求获取菜品详情"):
            response = requests.get(f"{BASE_URL}/user/dish/1")
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0


class TestCartAPI:
    """购物车模块接口测试"""
    
    @allure.feature("购物车")
    @allure.story("添加购物车")
    def test_add_cart(self):
        """测试添加购物车接口"""
        with allure.step("请求添加购物车"):
            data = {"dishId": 1, "number": 1}
            response = requests.post(
                f"{BASE_URL}/user/cart/add",
                headers=HEADERS,
                data=json.dumps(data)
            )
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0

    @allure.feature("购物车")
    @allure.story("获取购物车列表")
    def test_get_cart_list(self):
        """测试获取购物车列表接口"""
        with allure.step("请求获取购物车列表"):
            response = requests.get(f"{BASE_URL}/user/cart/list")
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0
            assert isinstance(result["data"], list)

    @allure.feature("购物车")
    @allure.story("清空购物车")
    def test_clean_cart(self):
        """测试清空购物车接口"""
        with allure.step("请求清空购物车"):
            response = requests.delete(f"{BASE_URL}/user/cart/clean")
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0


class TestOrderAPI:
    """订单模块接口测试"""
    
    @allure.feature("订单")
    @allure.story("提交订单")
    def test_submit_order(self):
        """测试提交订单接口"""
        with allure.step("准备订单数据"):
            data = {
                "addressBookId": 1,
                "remark": "测试订单",
                "orderDetailList": [
                    {"dishId": 1, "name": "测试菜品", "number": 1, "amount": 10.0}
                ]
            }
        
        with allure.step("请求提交订单"):
            response = requests.post(
                f"{BASE_URL}/user/order/submit",
                headers=HEADERS,
                data=json.dumps(data)
            )
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0

    @allure.feature("订单")
    @allure.story("查询订单列表")
    def test_get_order_list(self):
        """测试查询订单列表接口"""
        with allure.step("请求获取订单列表"):
            response = requests.get(
                f"{BASE_URL}/user/order/list",
                params={"page": 1, "pageSize": 10}
            )
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0


class TestAddressAPI:
    """地址模块接口测试"""
    
    @allure.feature("地址管理")
    @allure.story("获取地址列表")
    def test_get_address_list(self):
        """测试获取地址列表接口"""
        with allure.step("请求获取地址列表"):
            response = requests.get(f"{BASE_URL}/user/addressBook/list")
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0
            assert isinstance(result["data"], list)

    @allure.feature("地址管理")
    @allure.story("添加地址")
    def test_add_address(self):
        """测试添加地址接口"""
        with allure.step("准备地址数据"):
            data = {
                "consignee": "测试用户",
                "phone": "13800138000",
                "provinceCode": "110000",
                "cityCode": "110100",
                "districtCode": "110101",
                "detail": "测试地址详情"
            }
        
        with allure.step("请求添加地址"):
            response = requests.post(
                f"{BASE_URL}/user/addressBook/add",
                headers=HEADERS,
                data=json.dumps(data)
            )
        
        with allure.step("验证响应"):
            assert response.status_code == 200
            result = response.json()
            assert result["code"] == 0


if __name__ == "__main__":
    # 运行所有测试并生成Allure报告
    pytest.main([
        __file__,
        "--alluredir=allure-results",
        "-v",
        "--tb=short"
    ])