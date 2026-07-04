#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自动化截图脚本
功能：自动打开浏览器访问指定页面并截图保存
使用方法：python auto_screenshot.py

注意：运行前请确保前端服务已启动（http://localhost:5173）
      需要安装依赖：pip install selenium webdriver-manager
"""

import os
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from webdriver_manager.chrome import ChromeDriverManager

SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), 'screenshots', '09_页面展示')

os.makedirs(SCREENSHOT_DIR, exist_ok=True)

def init_driver():
    options = webdriver.ChromeOptions()
    options.add_argument('--start-maximized')
    options.add_argument('--disable-infobars')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-gpu')
    options.add_argument('--window-size=1920,1080')
    
    driver = webdriver.Chrome(
        service=Service(ChromeDriverManager().install()),
        options=options
    )
    driver.implicitly_wait(10)
    return driver

def login(driver):
    print("正在登录商家后台...")
    driver.get("http://localhost:5173")
    time.sleep(2)
    
    try:
        username_input = driver.find_element(By.CSS_SELECTOR, 'input[placeholder*="账号"], input[placeholder*="用户名"], input[name="username"], .login-form input:first-child')
        password_input = driver.find_element(By.CSS_SELECTOR, 'input[placeholder*="密码"], input[name="password"], .login-form input:last-child')
        
        username_input.send_keys('cyh')
        password_input.send_keys('123456')
        
        login_button = driver.find_element(By.CSS_SELECTOR, 'button[type="submit"], .login-btn, .el-button--primary')
        login_button.click()
        time.sleep(3)
        print("登录成功！")
    except Exception as e:
        print(f"登录失败，尝试其他选择器: {e}")
        print("请检查登录页面元素结构，可能需要调整选择器")

def capture_screenshot(driver, filename, description):
    filepath = os.path.join(SCREENSHOT_DIR, filename)
    driver.save_screenshot(filepath)
    print(f"✓ 已保存截图: {description} -> {filename}")

def click_menu(driver, menu_name):
    try:
        menu_link = driver.find_element(By.LINK_TEXT, menu_name)
        menu_link.click()
        time.sleep(2)
        return True
    except:
        try:
            menu_item = driver.find_element(By.XPATH, f'//*[text()="{menu_name}" or contains(text(),"{menu_name}")]')
            menu_item.click()
            time.sleep(2)
            return True
        except:
            print(f"⚠ 未找到菜单: {menu_name}，跳过")
            return False

def main():
    driver = None
    try:
        driver = init_driver()
        login(driver)
        
        capture_screenshot(driver, '09_01_仪表盘.png', '仪表盘首页')
        
        if click_menu(driver, '菜品管理'):
            capture_screenshot(driver, '09_03_菜品管理.png', '菜品管理页面')
        
        if click_menu(driver, '套餐管理'):
            capture_screenshot(driver, '09_04_套餐管理.png', '套餐管理页面')
        
        if click_menu(driver, '订单管理'):
            capture_screenshot(driver, '09_05_订单管理.png', '订单管理页面')
        
        if click_menu(driver, '数据统计'):
            capture_screenshot(driver, '09_06_数据统计.png', '数据统计页面')
        
        if click_menu(driver, '员工管理'):
            capture_screenshot(driver, '09_07_员工管理.png', '员工管理页面')
        
        if click_menu(driver, '店铺管理'):
            capture_screenshot(driver, '09_02_店铺管理.png', '店铺管理页面')
        
        print("\n✅ 所有截图已完成！")
        print(f"截图保存位置: {SCREENSHOT_DIR}")
        
    except Exception as e:
        print(f"❌ 出错了: {e}")
        import traceback
        traceback.print_exc()
    finally:
        if driver:
            driver.quit()
            print("浏览器已关闭")

if __name__ == '__main__':
    main()
