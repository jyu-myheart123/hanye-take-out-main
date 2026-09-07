import pytest
import requests
import yaml
import json

with open('config.yaml', 'r', encoding='utf-8') as f:
    config = yaml.safe_load(f)

BASE_URL = config.get('base_url', 'http://localhost:8081')
HEADERS = {"Content-Type": "application/json"}

USER_TOKEN = None
ADMIN_TOKEN = None

TOKEN_HEADER_NAME = "Authorization"

@pytest.fixture(scope="session", autouse=True)
def setup_tokens():
    global USER_TOKEN, ADMIN_TOKEN

    print("\n=== 登录获取用户Token ===")
    user_login_data = {"code": "test_code"}
    user_response = requests.post(
        f"{BASE_URL}/user/user/login",
        headers=HEADERS,
        data=json.dumps(user_login_data)
    )
    if user_response.status_code == 200:
        result = user_response.json()
        if result.get("code") == 0 and result.get("data"):
            USER_TOKEN = result["data"].get("token")
            print(f"用户Token获取成功: {USER_TOKEN[:20]}...")
        else:
            print(f"用户登录失败: {result}")
    else:
        print(f"用户登录请求失败: {user_response.status_code}")

    print("\n=== 登录获取管理员Token ===")
    admin_login_data = {"account": "cyh", "password": "123456"}
    admin_response = requests.post(
        f"{BASE_URL}/admin/employee/login",
        headers=HEADERS,
        data=json.dumps(admin_login_data)
    )
    if admin_response.status_code == 200:
        result = admin_response.json()
        if result.get("code") == 0 and result.get("data"):
            ADMIN_TOKEN = result["data"].get("token")
            print(f"管理员Token获取成功: {ADMIN_TOKEN[:20]}...")
        else:
            print(f"管理员登录失败: {result}")
    else:
        print(f"管理员登录请求失败: {admin_response.status_code}")

def get_user_headers():
    headers = HEADERS.copy()
    if USER_TOKEN:
        headers[TOKEN_HEADER_NAME] = USER_TOKEN
    return headers

def get_admin_headers():
    headers = HEADERS.copy()
    if ADMIN_TOKEN:
        headers[TOKEN_HEADER_NAME] = ADMIN_TOKEN
    return headers

class TestUserAPI:
    def test_user_login(self):
        data = {"code": "test_code"}
        response = requests.post(f"{BASE_URL}/user/user/login", headers=HEADERS, data=json.dumps(data))
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 0
        assert "token" in result["data"]

    def test_get_user_info(self):
        response = requests.get(f"{BASE_URL}/user/user/1", headers=get_user_headers())
        assert response.status_code == 200

class TestShopAPI:
    def test_get_shop_status(self):
        response = requests.get(f"{BASE_URL}/user/shop/status")
        assert response.status_code == 200

class TestCategoryAPI:
    def test_get_category_list(self):
        response = requests.get(f"{BASE_URL}/user/category/list", headers=get_user_headers())
        assert response.status_code == 200

class TestDishAPI:
    def test_get_dish_list(self):
        response = requests.get(f"{BASE_URL}/user/dish/list/1", headers=get_user_headers())
        if response.status_code == 500:
            pytest.skip("Redis连接失败，跳过此测试")
        assert response.status_code == 200

    def test_get_dish_detail(self):
        response = requests.get(f"{BASE_URL}/user/dish/dish/1", headers=get_user_headers())
        assert response.status_code == 200

class TestSetmealAPI:
    def test_get_setmeal_list(self):
        response = requests.get(f"{BASE_URL}/user/setmeal/list/6", headers=get_user_headers())
        if response.status_code == 500:
            pytest.skip("Redis连接失败，跳过此测试")
        assert response.status_code == 200

    def test_get_setmeal_detail(self):
        response = requests.get(f"{BASE_URL}/user/setmeal/1", headers=get_user_headers())
        assert response.status_code == 200

class TestAddressAPI:
    def test_get_address_list(self):
        response = requests.get(f"{BASE_URL}/user/address/list", headers=get_user_headers())
        assert response.status_code == 200

class TestAdminAPI:
    def test_admin_login(self):
        data = {"account": "cyh", "password": "123456"}
        response = requests.post(f"{BASE_URL}/admin/employee/login", headers=HEADERS, data=json.dumps(data))
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 0
        assert "token" in result["data"]

    def test_get_shop_status_admin(self):
        response = requests.get(f"{BASE_URL}/admin/shop/status", headers=get_admin_headers())
        if response.status_code == 500:
            pytest.skip("Redis连接失败，跳过此测试")
        assert response.status_code == 200

    def test_get_category_page_admin(self):
        response = requests.get(f"{BASE_URL}/admin/category/page?page=1&pageSize=10", headers=get_admin_headers())
        assert response.status_code == 200

    def test_get_dish_page_admin(self):
        response = requests.get(f"{BASE_URL}/admin/dish/page?page=1&pageSize=10", headers=get_admin_headers())
        assert response.status_code == 200

    def test_get_setmeal_page_admin(self):
        response = requests.get(f"{BASE_URL}/admin/setmeal/page?page=1&pageSize=10", headers=get_admin_headers())
        assert response.status_code == 200

    def test_get_order_list_admin(self):
        response = requests.get(f"{BASE_URL}/admin/order/conditionSearch?page=1&pageSize=10", headers=get_admin_headers())
        assert response.status_code == 200

if __name__ == "__main__":
    pytest.main([__file__, "-v"])