package fun.cyhgraph.controller.user;

import fun.cyhgraph.entity.AddressBook;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.AddressBookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("地址簿控制器单元测试")
class AddressBookControllerTest {

    @Mock
    private AddressBookService addressBookService;

    @InjectMocks
    private AddressBookController addressBookController;

    @Test
    @DisplayName("ADDR-001: 测试新增地址")
    void testAddAddress() {
        AddressBook addressBook = new AddressBook();
        addressBook.setPhone("13800138000");
        
        Result result = addressBookController.addAddress(addressBook);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(addressBookService).addAddress(addressBook);
    }

    @Test
    @DisplayName("ADDR-002: 测试获取地址列表")
    void testGetAddressList() {
        List<AddressBook> addressList = new ArrayList<>();
        AddressBook address = new AddressBook();
        address.setId(1);
        address.setPhone("13800138000");
        addressList.add(address);
        
        when(addressBookService.list(any())).thenReturn(addressList);
        
        Result<List<AddressBook>> result = addressBookController.list();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("ADDR-003: 测试获取默认地址")
    void testGetDefaultAddress() {
        List<AddressBook> addressList = new ArrayList<>();
        AddressBook address = new AddressBook();
        address.setId(1);
        address.setIsDefault(1);
        addressList.add(address);
        
        when(addressBookService.list(any())).thenReturn(addressList);
        
        Result<AddressBook> result = addressBookController.defaultAddress();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("ADDR-004: 测试无默认地址")
    void testGetNoDefaultAddress() {
        when(addressBookService.list(any())).thenReturn(new ArrayList<>());
        
        Result<AddressBook> result = addressBookController.defaultAddress();
        
        assertNotNull(result);
        assertEquals(1, result.getCode());
    }

    @Test
    @DisplayName("ADDR-005: 测试根据ID查询地址")
    void testGetAddressById() {
        AddressBook address = new AddressBook();
        address.setId(1);
        
        when(addressBookService.getById(1)).thenReturn(address);
        
        Result<AddressBook> result = addressBookController.getById(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("ADDR-006: 测试修改地址")
    void testUpdateAddress() {
        AddressBook addressBook = new AddressBook();
        addressBook.setId(1);
        
        Result result = addressBookController.updateAddress(addressBook);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(addressBookService).updateAddress(addressBook);
    }

    @Test
    @DisplayName("ADDR-007: 测试设置默认地址")
    void testSetDefaultAddress() {
        AddressBook addressBook = new AddressBook();
        addressBook.setId(1);
        
        Result result = addressBookController.setDefaultAddress(addressBook);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(addressBookService).setDefault(addressBook);
    }

    @Test
    @DisplayName("ADDR-008: 测试删除地址")
    void testDeleteAddress() {
        Result result = addressBookController.deleteAddress(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(addressBookService).deleteById(1);
    }
}