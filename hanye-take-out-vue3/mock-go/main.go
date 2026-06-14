package main

import (
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math/rand"
	"net"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

const timeLayout = "2006-01-02 15:04:05"

type apiResponse struct {
	Code int         `json:"code"`
	Msg  string      `json:"msg"`
	Data interface{} `json:"data,omitempty"`
}

type pageResult[T any] struct {
	Total   int `json:"total"`
	Records []T `json:"records"`
}

type employee struct {
	ID         int    `json:"id"`
	Name       string `json:"name"`
	Account    string `json:"account"`
	Password   string `json:"-"`
	Phone      string `json:"phone"`
	Age        int    `json:"age"`
	Gender     int    `json:"gender"`
	Pic        string `json:"pic"`
	Status     int    `json:"status"`
	UpdateTime string `json:"updateTime"`
}

type userInfo struct {
	ID      int    `json:"id"`
	Account string `json:"account"`
	Token   string `json:"token"`
}

type category struct {
	ID         int    `json:"id"`
	Name       string `json:"name"`
	Type       int    `json:"type"`
	Sort       int    `json:"sort"`
	Status     int    `json:"status"`
	UpdateTime string `json:"updateTime"`
}

type flavor struct {
	Name string `json:"name"`
	List string `json:"list"`
}

type dish struct {
	ID         int      `json:"id"`
	Name       string   `json:"name"`
	Pic        string   `json:"pic"`
	Detail     string   `json:"detail"`
	Price      float64  `json:"price"`
	Status     int      `json:"status"`
	CategoryID int      `json:"categoryId"`
	UpdateTime string   `json:"updateTime"`
	Flavors    []flavor `json:"flavors,omitempty"`
}

type setmealDish struct {
	DishID int     `json:"dishId"`
	Name   string  `json:"name"`
	Price  float64 `json:"price"`
	Copies int     `json:"copies"`
}

type setmeal struct {
	ID            int           `json:"id"`
	Name          string        `json:"name"`
	Pic           string        `json:"pic"`
	Detail        string        `json:"detail"`
	Price         float64       `json:"price"`
	Status        int           `json:"status"`
	CategoryID    int           `json:"categoryId"`
	UpdateTime    string        `json:"updateTime"`
	SetmealDishes []setmealDish `json:"setmealDishes,omitempty"`
}

type orderDetail struct {
	ID         int     `json:"id"`
	Name       string  `json:"name"`
	OrderID    int     `json:"orderId"`
	DishID     int     `json:"dishId"`
	SetmealID  int     `json:"setmealId"`
	DishFlavor string  `json:"dishFlavor"`
	Number     int     `json:"number"`
	Amount     float64 `json:"amount"`
	Pic        string  `json:"pic"`
}

type orderVO struct {
	ID                    int           `json:"id"`
	Number                string        `json:"number"`
	Status                int           `json:"status"`
	UserID                int           `json:"userId"`
	AddressBookID         int           `json:"addressBookId"`
	OrderTime             string        `json:"orderTime"`
	CheckoutTime          string        `json:"checkoutTime"`
	PayMethod             int           `json:"payMethod"`
	PayStatus             int           `json:"payStatus"`
	Amount                float64       `json:"amount"`
	Remark                string        `json:"remark"`
	UserName              string        `json:"userName"`
	Phone                 string        `json:"phone"`
	Address               string        `json:"address"`
	Consignee             string        `json:"consignee"`
	CancelReason          string        `json:"cancelReason"`
	RejectionReason       string        `json:"rejectionReason"`
	CancelTime            string        `json:"cancelTime"`
	EstimatedDeliveryTime string        `json:"estimatedDeliveryTime"`
	DeliveryStatus        int           `json:"deliveryStatus"`
	DeliveryTime          string        `json:"deliveryTime"`
	PackAmount            float64       `json:"packAmount"`
	TablewareNumber       int           `json:"tablewareNumber"`
	TablewareStatus       int           `json:"tablewareStatus"`
	OrderDishes           string        `json:"orderDishes"`
	OrderDetailList       []orderDetail `json:"orderDetailList"`
}

type mockStore struct {
	mu sync.RWMutex

	shopStatus int

	nextEmployeeID int
	nextCategoryID int
	nextDishID     int
	nextSetmealID  int
	nextOrderID    int

	employees  []employee
	categories []category
	dishes     []dish
	setmeals   []setmeal
	orders     []orderVO
}

func main() {
	rand.Seed(time.Now().UnixNano())
	store := seedStore()

	mux := http.NewServeMux()
	mux.HandleFunc("/", store.handleRequest)

	addr := ":8081"
	log.Printf("mock backend listening on http://localhost%s", addr)
	log.Printf("vite proxy target should stay: http://localhost:8081/admin")
	log.Fatal(http.ListenAndServe(addr, mux))
}

func (s *mockStore) handleRequest(w http.ResponseWriter, r *http.Request) {
	setCommonHeaders(w)

	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	if strings.HasPrefix(r.URL.Path, "/ws/") {
		s.handleWebSocket(w, r)
		return
	}

	if r.URL.Path == "/" {
		writeJSON(w, http.StatusOK, apiResponse{
			Code: 0,
			Msg:  "success",
			Data: map[string]string{
				"name":    "hanye take out mock backend",
				"version": "1.0.0",
			},
		})
		return
	}

	if !strings.HasPrefix(r.URL.Path, "/admin") {
		writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "path not found"})
		return
	}

	path := strings.TrimPrefix(r.URL.Path, "/admin")

	switch {
	case path == "/employee/login" && r.Method == http.MethodPost:
		s.handleLogin(w, r)
	case path == "/employee/register" && r.Method == http.MethodPost:
		s.handleRegister(w, r)
	case path == "/employee/fixpwd" && r.Method == http.MethodPut:
		s.handleFixPassword(w, r)
	case path == "/employee/add" && r.Method == http.MethodPost:
		s.handleEmployeeAdd(w, r)
	case path == "/employee/page" && r.Method == http.MethodGet:
		s.handleEmployeePage(w, r)
	case path == "/employee/update" && r.Method == http.MethodPut:
		s.handleEmployeeUpdate(w, r)
	case strings.HasPrefix(path, "/employee/status/") && r.Method == http.MethodPut:
		s.handleEmployeeToggleStatus(w, r, strings.TrimPrefix(path, "/employee/status/"))
	case strings.HasPrefix(path, "/employee/delete/") && r.Method == http.MethodDelete:
		s.handleEmployeeDelete(w, r, strings.TrimPrefix(path, "/employee/delete/"))
	case strings.HasPrefix(path, "/employee/") && r.Method == http.MethodGet:
		s.handleEmployeeByID(w, r, strings.TrimPrefix(path, "/employee/"))

	case path == "/shop/status" && r.Method == http.MethodGet:
		s.handleShopStatus(w)
	case strings.HasPrefix(path, "/shop/") && r.Method == http.MethodPut:
		s.handleShopSetStatus(w, strings.TrimPrefix(path, "/shop/"))

	case path == "/category" && r.Method == http.MethodPost:
		s.handleCategoryAdd(w, r)
	case path == "/category/page" && r.Method == http.MethodGet:
		s.handleCategoryPage(w, r)
	case path == "/category" && r.Method == http.MethodPut:
		s.handleCategoryUpdate(w, r)
	case strings.HasPrefix(path, "/category/status/") && r.Method == http.MethodPut:
		s.handleCategoryToggleStatus(w, strings.TrimPrefix(path, "/category/status/"))
	case strings.HasPrefix(path, "/category/") && r.Method == http.MethodDelete:
		s.handleCategoryDelete(w, strings.TrimPrefix(path, "/category/"))
	case strings.HasPrefix(path, "/category/") && r.Method == http.MethodGet:
		s.handleCategoryByID(w, strings.TrimPrefix(path, "/category/"))

	case path == "/dish" && r.Method == http.MethodPost:
		s.handleDishAdd(w, r)
	case path == "/dish/page" && r.Method == http.MethodGet:
		s.handleDishPage(w, r)
	case path == "/dish" && r.Method == http.MethodPut:
		s.handleDishUpdate(w, r)
	case path == "/dish" && r.Method == http.MethodDelete:
		s.handleDishDelete(w, r)
	case strings.HasPrefix(path, "/dish/status/") && r.Method == http.MethodPut:
		s.handleDishToggleStatus(w, strings.TrimPrefix(path, "/dish/status/"))
	case strings.HasPrefix(path, "/dish/") && r.Method == http.MethodGet:
		s.handleDishByID(w, strings.TrimPrefix(path, "/dish/"))

	case path == "/setmeal" && r.Method == http.MethodPost:
		s.handleSetmealAdd(w, r)
	case path == "/setmeal/page" && r.Method == http.MethodGet:
		s.handleSetmealPage(w, r)
	case path == "/setmeal" && r.Method == http.MethodPut:
		s.handleSetmealUpdate(w, r)
	case path == "/setmeal" && r.Method == http.MethodDelete:
		s.handleSetmealDelete(w, r)
	case strings.HasPrefix(path, "/setmeal/status/") && r.Method == http.MethodPut:
		s.handleSetmealToggleStatus(w, strings.TrimPrefix(path, "/setmeal/status/"))
	case strings.HasPrefix(path, "/setmeal/") && r.Method == http.MethodGet:
		s.handleSetmealByID(w, strings.TrimPrefix(path, "/setmeal/"))

	case path == "/workspace/overviewOrders" && r.Method == http.MethodGet:
		s.handleWorkspaceOverviewOrders(w)
	case path == "/workspace/overviewDishes" && r.Method == http.MethodGet:
		s.handleWorkspaceOverviewDishes(w)
	case path == "/workspace/overviewSetmeals" && r.Method == http.MethodGet:
		s.handleWorkspaceOverviewSetmeals(w)
	case path == "/workspace/businessData" && r.Method == http.MethodGet:
		s.handleWorkspaceBusinessData(w)

	case path == "/order/conditionSearch" && r.Method == http.MethodGet:
		s.handleOrderPage(w, r)
	case path == "/order/statistics" && r.Method == http.MethodGet:
		s.handleOrderStatistics(w)
	case path == "/order/cancel" && r.Method == http.MethodPut:
		s.handleOrderCancel(w, r)
	case path == "/order/confirm" && r.Method == http.MethodPut:
		s.handleOrderConfirm(w, r)
	case path == "/order/reject" && r.Method == http.MethodPut:
		s.handleOrderReject(w, r)
	case strings.HasPrefix(path, "/order/details/") && r.Method == http.MethodGet:
		s.handleOrderDetail(w, strings.TrimPrefix(path, "/order/details/"))
	case strings.HasPrefix(path, "/order/delivery/") && r.Method == http.MethodPut:
		s.handleOrderDelivery(w, strings.TrimPrefix(path, "/order/delivery/"))
	case strings.HasPrefix(path, "/order/complete/") && r.Method == http.MethodPut:
		s.handleOrderComplete(w, strings.TrimPrefix(path, "/order/complete/"))

	case path == "/report/turnoverStatistics" && r.Method == http.MethodGet:
		s.handleTurnoverStatistics(w, r)
	case path == "/report/userStatistics" && r.Method == http.MethodGet:
		s.handleUserStatistics(w, r)
	case path == "/report/orderStatistics" && r.Method == http.MethodGet:
		s.handleOrderTrendStatistics(w, r)
	case path == "/report/top10Statistics" && r.Method == http.MethodGet:
		s.handleTop10Statistics(w)
	case path == "/report/dataOverView" && r.Method == http.MethodGet:
		s.handleDataOverview(w)
	case path == "/report/export" && r.Method == http.MethodGet:
		s.handleExport(w)
	case strings.HasPrefix(path, "/report/"):
		s.handleGenericReport(w, path)

	default:
		writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "path not found"})
	}
}

func (s *mockStore) handleLogin(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Account  string `json:"account"`
		Password string `json:"password"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	for _, item := range s.employees {
		if item.Account == req.Account {
			writeJSON(w, http.StatusOK, apiResponse{
				Code: 0,
				Msg:  "success",
				Data: userInfo{
					ID:      item.ID,
					Account: item.Account,
					Token:   "mock-token-" + item.Account,
				},
			})
			return
		}
	}

	account := req.Account
	if strings.TrimSpace(account) == "" {
		account = "cyh"
	}
	writeJSON(w, http.StatusOK, apiResponse{
		Code: 0,
		Msg:  "success",
		Data: userInfo{
			ID:      1,
			Account: account,
			Token:   "mock-token-" + account,
		},
	})
}

func (s *mockStore) handleRegister(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Account    string `json:"account"`
		Password   string `json:"password"`
		Repassword string `json:"repassword"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	now := nowString()
	s.nextEmployeeID++
	s.employees = append(s.employees, employee{
		ID:         s.nextEmployeeID,
		Name:       "测试用户" + strconv.Itoa(s.nextEmployeeID),
		Account:    defaultString(req.Account, "mockuser"),
		Password:   req.Password,
		Phone:      randomPhone(),
		Age:        22,
		Gender:     s.nextEmployeeID % 2,
		Pic:        placeholderPic("User", "#df6d26"),
		Status:     1,
		UpdateTime: now,
	})

	writeSuccess(w, nil)
}

func (s *mockStore) handleFixPassword(w http.ResponseWriter, r *http.Request) {
	writeSuccess(w, nil)
}

func (s *mockStore) handleEmployeeAdd(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name     string      `json:"name"`
		Account  string      `json:"account"`
		Password string      `json:"password"`
		Phone    string      `json:"phone"`
		Age      interface{} `json:"age"`
		Gender   interface{} `json:"gender"`
		Pic      string      `json:"pic"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	s.nextEmployeeID++
	s.employees = append(s.employees, employee{
		ID:         s.nextEmployeeID,
		Name:       defaultString(req.Name, "新员工"),
		Account:    defaultString(req.Account, fmt.Sprintf("user%d", s.nextEmployeeID)),
		Password:   defaultString(req.Password, "123456"),
		Phone:      defaultString(req.Phone, randomPhone()),
		Age:        intFromAny(req.Age, 20),
		Gender:     intFromAny(req.Gender, 1),
		Pic:        defaultString(req.Pic, placeholderPic("User", "#2f8f83")),
		Status:     1,
		UpdateTime: nowString(),
	})

	writeSuccess(w, nil)
}

func (s *mockStore) handleEmployeePage(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	name := strings.TrimSpace(r.URL.Query().Get("name"))
	filtered := make([]employee, 0, len(s.employees))
	for _, item := range s.employees {
		if name != "" && !strings.Contains(strings.ToLower(item.Name), strings.ToLower(name)) &&
			!strings.Contains(strings.ToLower(item.Account), strings.ToLower(name)) {
			continue
		}
		filtered = append(filtered, item)
	}

	page, pageSize := queryPage(r)
	writeJSON(w, http.StatusOK, apiResponse{
		Code: 0,
		Msg:  "success",
		Data: paginate(filtered, page, pageSize),
	})
}

func (s *mockStore) handleEmployeeByID(w http.ResponseWriter, r *http.Request, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	for _, item := range s.employees {
		if item.ID == id {
			writeSuccess(w, item)
			return
		}
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "employee not found"})
}

func (s *mockStore) handleEmployeeUpdate(w http.ResponseWriter, r *http.Request) {
	var req map[string]interface{}
	if !decodeJSON(w, r, &req) {
		return
	}

	id := intFromAny(req["id"], 0)
	s.mu.Lock()
	defer s.mu.Unlock()

	for i, item := range s.employees {
		if item.ID != id {
			continue
		}
		if v, ok := req["name"].(string); ok {
			item.Name = v
		}
		if v, ok := req["account"].(string); ok {
			item.Account = v
		}
		if v, ok := req["phone"].(string); ok {
			item.Phone = v
		}
		if _, ok := req["age"]; ok {
			item.Age = intFromAny(req["age"], item.Age)
		}
		if _, ok := req["gender"]; ok {
			item.Gender = intFromAny(req["gender"], item.Gender)
		}
		if v, ok := req["pic"].(string); ok && strings.TrimSpace(v) != "" {
			item.Pic = v
		}
		item.UpdateTime = nowString()
		s.employees[i] = item
		writeSuccess(w, nil)
		return
	}

	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "employee not found"})
}

func (s *mockStore) handleEmployeeToggleStatus(w http.ResponseWriter, r *http.Request, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.employees {
		if item.ID != id {
			continue
		}
		if item.Status == 1 {
			item.Status = 0
		} else {
			item.Status = 1
		}
		item.UpdateTime = nowString()
		s.employees[i] = item
		writeSuccess(w, nil)
		return
	}

	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "employee not found"})
}

func (s *mockStore) handleEmployeeDelete(w http.ResponseWriter, r *http.Request, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	for i, item := range s.employees {
		if item.ID != id {
			continue
		}
		s.employees = append(s.employees[:i], s.employees[i+1:]...)
		writeSuccess(w, nil)
		return
	}

	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "employee not found"})
}

func (s *mockStore) handleShopStatus(w http.ResponseWriter) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	writeSuccess(w, s.shopStatus)
}

func (s *mockStore) handleShopSetStatus(w http.ResponseWriter, statusText string) {
	status, ok := parseID(w, statusText)
	if !ok {
		return
	}

	s.mu.Lock()
	s.shopStatus = status
	s.mu.Unlock()
	writeSuccess(w, nil)
}

func (s *mockStore) handleCategoryAdd(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name string      `json:"name"`
		Type interface{} `json:"type"`
		Sort interface{} `json:"sort"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	s.nextCategoryID++
	s.categories = append(s.categories, category{
		ID:         s.nextCategoryID,
		Name:       defaultString(req.Name, "新分类"),
		Type:       intFromAny(req.Type, 1),
		Sort:       intFromAny(req.Sort, 1),
		Status:     1,
		UpdateTime: nowString(),
	})
	writeSuccess(w, nil)
}

func (s *mockStore) handleCategoryPage(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	name := strings.TrimSpace(r.URL.Query().Get("name"))
	typeFilter := intQuery(r, "type", 0)

	filtered := make([]category, 0, len(s.categories))
	for _, item := range s.categories {
		if name != "" && !strings.Contains(strings.ToLower(item.Name), strings.ToLower(name)) {
			continue
		}
		if typeFilter != 0 && item.Type != typeFilter {
			continue
		}
		filtered = append(filtered, item)
	}

	sort.Slice(filtered, func(i, j int) bool {
		if filtered[i].Sort == filtered[j].Sort {
			return filtered[i].ID < filtered[j].ID
		}
		return filtered[i].Sort < filtered[j].Sort
	})

	page, pageSize := queryPage(r)
	writeSuccess(w, paginate(filtered, page, pageSize))
}

func (s *mockStore) handleCategoryByID(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.RLock()
	defer s.mu.RUnlock()
	for _, item := range s.categories {
		if item.ID == id {
			writeSuccess(w, item)
			return
		}
	}

	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "category not found"})
}

func (s *mockStore) handleCategoryUpdate(w http.ResponseWriter, r *http.Request) {
	var req map[string]interface{}
	if !decodeJSON(w, r, &req) {
		return
	}
	id := intFromAny(req["id"], 0)

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.categories {
		if item.ID != id {
			continue
		}
		if v, ok := req["name"].(string); ok {
			item.Name = v
		}
		if _, ok := req["type"]; ok {
			item.Type = intFromAny(req["type"], item.Type)
		}
		if _, ok := req["sort"]; ok {
			item.Sort = intFromAny(req["sort"], item.Sort)
		}
		item.UpdateTime = nowString()
		s.categories[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "category not found"})
}

func (s *mockStore) handleCategoryToggleStatus(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.categories {
		if item.ID != id {
			continue
		}
		if item.Status == 1 {
			item.Status = 0
		} else {
			item.Status = 1
		}
		item.UpdateTime = nowString()
		s.categories[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "category not found"})
}

func (s *mockStore) handleCategoryDelete(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.categories {
		if item.ID != id {
			continue
		}
		s.categories = append(s.categories[:i], s.categories[i+1:]...)
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "category not found"})
}

func (s *mockStore) handleDishAdd(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name       string        `json:"name"`
		Pic        string        `json:"pic"`
		Detail     string        `json:"detail"`
		Price      interface{}   `json:"price"`
		Status     interface{}   `json:"status"`
		CategoryID interface{}   `json:"categoryId"`
		Flavors    []flavorInput `json:"flavors"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	s.nextDishID++
	item := dish{
		ID:         s.nextDishID,
		Name:       defaultString(req.Name, "新菜品"),
		Pic:        defaultString(req.Pic, placeholderPic("Dish", "#d15f1f")),
		Detail:     defaultString(req.Detail, "用于测试的静态菜品"),
		Price:      floatFromAny(req.Price, 19.9),
		Status:     intFromAny(req.Status, 1),
		CategoryID: intFromAny(req.CategoryID, s.defaultDishCategoryID()),
		UpdateTime: nowString(),
		Flavors:    req.toFlavors(),
	}
	s.dishes = append(s.dishes, item)
	writeSuccess(w, nil)
}

func (s *mockStore) handleDishPage(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	name := strings.TrimSpace(r.URL.Query().Get("name"))
	categoryID := intQuery(r, "categoryId", 0)
	status := intQuery(r, "status", -1)

	filtered := make([]dish, 0, len(s.dishes))
	for _, item := range s.dishes {
		if name != "" && !strings.Contains(strings.ToLower(item.Name), strings.ToLower(name)) {
			continue
		}
		if categoryID != 0 && item.CategoryID != categoryID {
			continue
		}
		if status != -1 && item.Status != status {
			continue
		}
		filtered = append(filtered, item)
	}

	page, pageSize := queryPage(r)
	writeSuccess(w, paginate(filtered, page, pageSize))
}

func (s *mockStore) handleDishByID(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.RLock()
	defer s.mu.RUnlock()
	for _, item := range s.dishes {
		if item.ID == id {
			writeSuccess(w, item)
			return
		}
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "dish not found"})
}

func (s *mockStore) handleDishUpdate(w http.ResponseWriter, r *http.Request) {
	var req struct {
		ID         int           `json:"id"`
		Name       string        `json:"name"`
		Pic        string        `json:"pic"`
		Detail     string        `json:"detail"`
		Price      interface{}   `json:"price"`
		Status     interface{}   `json:"status"`
		CategoryID interface{}   `json:"categoryId"`
		Flavors    []flavorInput `json:"flavors"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.dishes {
		if item.ID != req.ID {
			continue
		}
		if strings.TrimSpace(req.Name) != "" {
			item.Name = req.Name
		}
		if strings.TrimSpace(req.Pic) != "" {
			item.Pic = req.Pic
		}
		if strings.TrimSpace(req.Detail) != "" {
			item.Detail = req.Detail
		}
		item.Price = floatFromAny(req.Price, item.Price)
		item.Status = intFromAny(req.Status, item.Status)
		item.CategoryID = intFromAny(req.CategoryID, item.CategoryID)
		if len(req.Flavors) > 0 {
			item.Flavors = req.toFlavors()
		}
		item.UpdateTime = nowString()
		s.dishes[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "dish not found"})
}

func (s *mockStore) handleDishToggleStatus(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.dishes {
		if item.ID != id {
			continue
		}
		if item.Status == 1 {
			item.Status = 0
		} else {
			item.Status = 1
		}
		item.UpdateTime = nowString()
		s.dishes[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "dish not found"})
}

func (s *mockStore) handleDishDelete(w http.ResponseWriter, r *http.Request) {
	ids := parseIDs(r.URL.Query().Get("ids"))
	if len(ids) == 0 {
		writeJSON(w, http.StatusBadRequest, apiResponse{Code: 400, Msg: "missing ids"})
		return
	}

	toDelete := make(map[int]struct{}, len(ids))
	for _, id := range ids {
		toDelete[id] = struct{}{}
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	filtered := s.dishes[:0]
	for _, item := range s.dishes {
		if _, ok := toDelete[item.ID]; ok {
			continue
		}
		filtered = append(filtered, item)
	}
	s.dishes = filtered
	writeSuccess(w, nil)
}

func (s *mockStore) handleSetmealAdd(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name          string        `json:"name"`
		Pic           string        `json:"pic"`
		Detail        string        `json:"detail"`
		Price         interface{}   `json:"price"`
		Status        interface{}   `json:"status"`
		CategoryID    interface{}   `json:"categoryId"`
		SetmealDishes []setmealDish `json:"setmealDishes"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	s.nextSetmealID++
	s.setmeals = append(s.setmeals, setmeal{
		ID:            s.nextSetmealID,
		Name:          defaultString(req.Name, "新套餐"),
		Pic:           defaultString(req.Pic, placeholderPic("Set", "#cc8b2e")),
		Detail:        defaultString(req.Detail, "用于测试的静态套餐"),
		Price:         floatFromAny(req.Price, 49.9),
		Status:        intFromAny(req.Status, 1),
		CategoryID:    intFromAny(req.CategoryID, s.defaultSetmealCategoryID()),
		UpdateTime:    nowString(),
		SetmealDishes: req.SetmealDishes,
	})
	writeSuccess(w, nil)
}

func (s *mockStore) handleSetmealPage(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	name := strings.TrimSpace(r.URL.Query().Get("name"))
	categoryID := intQuery(r, "categoryId", 0)
	status := intQuery(r, "status", -1)

	filtered := make([]setmeal, 0, len(s.setmeals))
	for _, item := range s.setmeals {
		if name != "" && !strings.Contains(strings.ToLower(item.Name), strings.ToLower(name)) {
			continue
		}
		if categoryID != 0 && item.CategoryID != categoryID {
			continue
		}
		if status != -1 && item.Status != status {
			continue
		}
		filtered = append(filtered, item)
	}

	page, pageSize := queryPage(r)
	writeSuccess(w, paginate(filtered, page, pageSize))
}

func (s *mockStore) handleSetmealByID(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.RLock()
	defer s.mu.RUnlock()
	for _, item := range s.setmeals {
		if item.ID == id {
			writeSuccess(w, item)
			return
		}
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "setmeal not found"})
}

func (s *mockStore) handleSetmealUpdate(w http.ResponseWriter, r *http.Request) {
	var req struct {
		ID            int           `json:"id"`
		Name          string        `json:"name"`
		Pic           string        `json:"pic"`
		Detail        string        `json:"detail"`
		Price         interface{}   `json:"price"`
		Status        interface{}   `json:"status"`
		CategoryID    interface{}   `json:"categoryId"`
		SetmealDishes []setmealDish `json:"setmealDishes"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.setmeals {
		if item.ID != req.ID {
			continue
		}
		if strings.TrimSpace(req.Name) != "" {
			item.Name = req.Name
		}
		if strings.TrimSpace(req.Pic) != "" {
			item.Pic = req.Pic
		}
		if strings.TrimSpace(req.Detail) != "" {
			item.Detail = req.Detail
		}
		item.Price = floatFromAny(req.Price, item.Price)
		item.Status = intFromAny(req.Status, item.Status)
		item.CategoryID = intFromAny(req.CategoryID, item.CategoryID)
		if len(req.SetmealDishes) > 0 {
			item.SetmealDishes = req.SetmealDishes
		}
		item.UpdateTime = nowString()
		s.setmeals[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "setmeal not found"})
}

func (s *mockStore) handleSetmealToggleStatus(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.setmeals {
		if item.ID != id {
			continue
		}
		if item.Status == 1 {
			item.Status = 0
		} else {
			item.Status = 1
		}
		item.UpdateTime = nowString()
		s.setmeals[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "setmeal not found"})
}

func (s *mockStore) handleSetmealDelete(w http.ResponseWriter, r *http.Request) {
	ids := parseIDs(r.URL.Query().Get("ids"))
	if len(ids) == 0 {
		writeJSON(w, http.StatusBadRequest, apiResponse{Code: 400, Msg: "missing ids"})
		return
	}

	toDelete := make(map[int]struct{}, len(ids))
	for _, id := range ids {
		toDelete[id] = struct{}{}
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	filtered := s.setmeals[:0]
	for _, item := range s.setmeals {
		if _, ok := toDelete[item.ID]; ok {
			continue
		}
		filtered = append(filtered, item)
	}
	s.setmeals = filtered
	writeSuccess(w, nil)
}

func (s *mockStore) handleWorkspaceOverviewOrders(w http.ResponseWriter) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	data := map[string]int{
		"waitingOrders":   s.countOrdersByStatus(2),
		"deliveredOrders": s.countOrdersByStatus(3),
		"completedOrders": s.countOrdersByStatus(5),
		"cancelledOrders": s.countOrdersByStatus(6),
		"allOrders":       len(s.orders),
	}
	writeSuccess(w, data)
}

func (s *mockStore) handleWorkspaceOverviewDishes(w http.ResponseWriter) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	sold := 0
	discontinued := 0
	for _, item := range s.dishes {
		if item.Status == 1 {
			sold++
		} else {
			discontinued++
		}
	}

	writeSuccess(w, map[string]int{
		"sold":         sold,
		"discontinued": discontinued,
	})
}

func (s *mockStore) handleWorkspaceOverviewSetmeals(w http.ResponseWriter) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	sold := 0
	discontinued := 0
	for _, item := range s.setmeals {
		if item.Status == 1 {
			sold++
		} else {
			discontinued++
		}
	}

	writeSuccess(w, map[string]int{
		"sold":         sold,
		"discontinued": discontinued,
	})
}

func (s *mockStore) handleWorkspaceBusinessData(w http.ResponseWriter) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	validOrderCount := s.countOrdersByStatus(5)
	totalTurnover := 0.0
	for _, item := range s.orders {
		if item.Status == 5 {
			totalTurnover += item.Amount
		}
	}

	completionRate := 0.0
	if len(s.orders) > 0 {
		completionRate = float64(validOrderCount) / float64(len(s.orders))
	}

	unitPrice := 0.0
	if validOrderCount > 0 {
		unitPrice = totalTurnover / float64(validOrderCount)
	}

	writeSuccess(w, map[string]interface{}{
		"turnover":            round2(totalTurnover),
		"validOrderCount":     validOrderCount,
		"orderCompletionRate": round4(completionRate),
		"unitPrice":           round2(unitPrice),
		"newUsers":            12,
	})
}

func (s *mockStore) handleOrderPage(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	status := intQuery(r, "status", 0)
	number := strings.TrimSpace(r.URL.Query().Get("number"))
	phone := strings.TrimSpace(r.URL.Query().Get("phone"))
	begin := strings.TrimSpace(r.URL.Query().Get("beginTime"))
	end := strings.TrimSpace(r.URL.Query().Get("endTime"))

	filtered := make([]orderVO, 0, len(s.orders))
	for _, item := range s.orders {
		if status != 0 && item.Status != status {
			continue
		}
		if number != "" && !strings.Contains(item.Number, number) {
			continue
		}
		if phone != "" && !strings.Contains(item.Phone, phone) {
			continue
		}
		if begin != "" && item.OrderTime < begin {
			continue
		}
		if end != "" && item.OrderTime > end {
			continue
		}
		filtered = append(filtered, item)
	}

	sort.Slice(filtered, func(i, j int) bool {
		return filtered[i].ID > filtered[j].ID
	})

	page, pageSize := queryPage(r)
	writeSuccess(w, paginate(filtered, page, pageSize))
}

func (s *mockStore) handleOrderDetail(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.RLock()
	defer s.mu.RUnlock()
	for _, item := range s.orders {
		if item.ID == id {
			writeSuccess(w, item)
			return
		}
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "order not found"})
}

func (s *mockStore) handleOrderDelivery(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.orders {
		if item.ID != id {
			continue
		}
		item.Status = 4
		item.UpdateDeliveryTimes(false)
		s.orders[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "order not found"})
}

func (s *mockStore) handleOrderComplete(w http.ResponseWriter, idText string) {
	id, ok := parseID(w, idText)
	if !ok {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.orders {
		if item.ID != id {
			continue
		}
		item.Status = 5
		item.UpdateDeliveryTimes(true)
		s.orders[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "order not found"})
}

func (s *mockStore) handleOrderCancel(w http.ResponseWriter, r *http.Request) {
	var req struct {
		ID           interface{} `json:"id"`
		CancelReason string      `json:"cancelReason"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	id := intFromAny(req.ID, 0)
	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.orders {
		if item.ID != id {
			continue
		}
		item.Status = 6
		item.CancelReason = defaultString(req.CancelReason, "商家主动取消")
		item.CancelTime = nowString()
		s.orders[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "order not found"})
}

func (s *mockStore) handleOrderConfirm(w http.ResponseWriter, r *http.Request) {
	var req struct {
		ID interface{} `json:"id"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	id := intFromAny(req.ID, 0)
	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.orders {
		if item.ID != id {
			continue
		}
		item.Status = 3
		s.orders[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "order not found"})
}

func (s *mockStore) handleOrderReject(w http.ResponseWriter, r *http.Request) {
	var req struct {
		ID              interface{} `json:"id"`
		RejectionReason string      `json:"rejectionReason"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}

	id := intFromAny(req.ID, 0)
	s.mu.Lock()
	defer s.mu.Unlock()
	for i, item := range s.orders {
		if item.ID != id {
			continue
		}
		item.Status = 6
		item.RejectionReason = defaultString(req.RejectionReason, "当前订单量较大")
		item.CancelTime = nowString()
		s.orders[i] = item
		writeSuccess(w, nil)
		return
	}
	writeJSON(w, http.StatusNotFound, apiResponse{Code: 404, Msg: "order not found"})
}

func (s *mockStore) handleOrderStatistics(w http.ResponseWriter) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	writeSuccess(w, map[string]int{
		"toBeConfirmed":     s.countOrdersByStatus(2),
		"confirmed":         s.countOrdersByStatus(3),
		"deliveryInProgress": s.countOrdersByStatus(4),
	})
}

func (s *mockStore) handleTurnoverStatistics(w http.ResponseWriter, r *http.Request) {
	begin, end := dateRange(r)
	dateList := dateListBetween(begin, end)
	values := make([]string, 0, len(dateList))
	for i := range dateList {
		values = append(values, strconv.FormatFloat(1800+float64(i*135), 'f', 2, 64))
	}

	writeSuccess(w, map[string]string{
		"dateList":     strings.Join(dateList, ","),
		"turnoverList": strings.Join(values, ","),
	})
}

func (s *mockStore) handleUserStatistics(w http.ResponseWriter, r *http.Request) {
	begin, end := dateRange(r)
	dateList := dateListBetween(begin, end)
	totalUsers := make([]string, 0, len(dateList))
	newUsers := make([]string, 0, len(dateList))
	base := 320
	for i := range dateList {
		totalUsers = append(totalUsers, strconv.Itoa(base+i*7))
		newUsers = append(newUsers, strconv.Itoa(5+i%6))
	}

	writeSuccess(w, map[string]string{
		"dateList":      strings.Join(dateList, ","),
		"totalUserList": strings.Join(totalUsers, ","),
		"newUserList":   strings.Join(newUsers, ","),
	})
}

func (s *mockStore) handleOrderTrendStatistics(w http.ResponseWriter, r *http.Request) {
	begin, end := dateRange(r)
	dateList := dateListBetween(begin, end)
	orderCount := make([]string, 0, len(dateList))
	validCount := make([]string, 0, len(dateList))
	total := 0
	valid := 0
	for i := range dateList {
		curTotal := 18 + i*3
		curValid := curTotal - (i % 3)
		total += curTotal
		valid += curValid
		orderCount = append(orderCount, strconv.Itoa(curTotal))
		validCount = append(validCount, strconv.Itoa(curValid))
	}

	rate := 0.0
	if total > 0 {
		rate = float64(valid) / float64(total)
	}

	writeSuccess(w, map[string]interface{}{
		"dateList":            strings.Join(dateList, ","),
		"orderCountList":      strings.Join(orderCount, ","),
		"validOrderCountList": strings.Join(validCount, ","),
		"totalOrderCount":     total,
		"validOrderCount":     valid,
		"orderCompletionRate": round4(rate),
	})
}

func (s *mockStore) handleTop10Statistics(w http.ResponseWriter) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	names := make([]string, 0, min(10, len(s.dishes)))
	values := make([]string, 0, min(10, len(s.dishes)))
	for i, item := range s.dishes {
		if i >= 10 {
			break
		}
		names = append(names, item.Name)
		values = append(values, strconv.Itoa(80-i*5))
	}

	writeSuccess(w, map[string]string{
		"nameList":   strings.Join(names, ","),
		"numberList": strings.Join(values, ","),
	})
}

func (s *mockStore) handleDataOverview(w http.ResponseWriter) {
	writeSuccess(w, map[string]interface{}{
		"turnover":        26880.5,
		"orderCount":      188,
		"userCount":       426,
		"newUserCount":    18,
		"validOrderCount": 166,
	})
}

func (s *mockStore) handleExport(w http.ResponseWriter) {
	content := "日期,营业额,订单数\n2026-06-10,1880.50,26\n2026-06-11,2050.80,31\n2026-06-12,2320.00,34\n"
	w.Header().Set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(content))
}

func (s *mockStore) handleGenericReport(w http.ResponseWriter, path string) {
	switch {
	case strings.Contains(path, "dayCollect"):
		writeSuccess(w, map[string]interface{}{
			"turnover":        2320.5,
			"orderCount":      34,
			"validOrderCount": 31,
		})
	case strings.Contains(path, "hourCollect"), strings.Contains(path, "dayAmountCollect"):
		writeSuccess(w, map[string]interface{}{
			"xAxis": []string{"08", "10", "12", "14", "16", "18", "20"},
			"yAxis": []int{80, 160, 220, 180, 260, 300, 210},
		})
	case strings.Contains(path, "payTypeCollect"):
		writeSuccess(w, []map[string]interface{}{
			{"name": "微信", "value": 68},
			{"name": "支付宝", "value": 32},
		})
	case strings.Contains(path, "privilege"):
		writeSuccess(w, map[string]interface{}{
			"couponAmount":   88.5,
			"discountAmount": 126.0,
			"memberAmount":   42.0,
		})
	case strings.Contains(path, "categoryCollect"):
		writeSuccess(w, []map[string]interface{}{
			{"name": "招牌菜", "value": 42},
			{"name": "饮品", "value": 28},
			{"name": "套餐", "value": 19},
		})
	case strings.Contains(path, "Rank"):
		writeSuccess(w, []map[string]interface{}{
			{"name": "香辣鸡腿堡", "value": 58},
			{"name": "双人快乐餐", "value": 43},
			{"name": "杨枝甘露", "value": 35},
		})
	default:
		writeSuccess(w, map[string]string{"message": "mock report data"})
	}
}

func (s *mockStore) handleWebSocket(w http.ResponseWriter, r *http.Request) {
	if !strings.EqualFold(r.Header.Get("Upgrade"), "websocket") {
		http.Error(w, "upgrade required", http.StatusUpgradeRequired)
		return
	}

	hijacker, ok := w.(http.Hijacker)
	if !ok {
		http.Error(w, "websocket not supported", http.StatusInternalServerError)
		return
	}

	conn, buf, err := hijacker.Hijack()
	if err != nil {
		http.Error(w, "hijack failed", http.StatusInternalServerError)
		return
	}

	acceptKey := computeAcceptKey(r.Header.Get("Sec-WebSocket-Key"))
	response := "HTTP/1.1 101 Switching Protocols\r\n" +
		"Upgrade: websocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n"

	if _, err := buf.WriteString(response); err != nil {
		_ = conn.Close()
		return
	}
	if err := buf.Flush(); err != nil {
		_ = conn.Close()
		return
	}

	go keepWebSocketAlive(conn)
}

func keepWebSocketAlive(conn net.Conn) {
	defer conn.Close()
	buffer := make([]byte, 512)
	for {
		_ = conn.SetReadDeadline(time.Now().Add(5 * time.Minute))
		if _, err := conn.Read(buffer); err != nil {
			return
		}
	}
}

func computeAcceptKey(key string) string {
	hash := sha1.Sum([]byte(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))
	return base64.StdEncoding.EncodeToString(hash[:])
}

type flavorInput struct {
	Name string      `json:"name"`
	List interface{} `json:"list"`
}

func (items []flavorInput) toFlavors() []flavor {
	result := make([]flavor, 0, len(items))
	for _, item := range items {
		switch value := item.List.(type) {
		case string:
			result = append(result, flavor{Name: item.Name, List: value})
		case []interface{}:
			raw, _ := json.Marshal(value)
			result = append(result, flavor{Name: item.Name, List: string(raw)})
		default:
			raw, _ := json.Marshal(value)
			result = append(result, flavor{Name: item.Name, List: string(raw)})
		}
	}
	return result
}

func (o *orderVO) UpdateDeliveryTimes(completed bool) {
	now := time.Now()
	if o.EstimatedDeliveryTime == "" {
		o.EstimatedDeliveryTime = now.Add(35 * time.Minute).Format(timeLayout)
	}
	if completed {
		o.DeliveryTime = now.Format(timeLayout)
	}
}

func seedStore() *mockStore {
	now := time.Now()
	nowText := now.Format(timeLayout)

	categories := []category{
		{ID: 1, Name: "招牌热菜", Type: 1, Sort: 1, Status: 1, UpdateTime: nowText},
		{ID: 2, Name: "饮品甜点", Type: 1, Sort: 2, Status: 1, UpdateTime: nowText},
		{ID: 3, Name: "双人套餐", Type: 2, Sort: 1, Status: 1, UpdateTime: nowText},
		{ID: 4, Name: "工作日午餐", Type: 2, Sort: 2, Status: 0, UpdateTime: nowText},
	}

	dishes := []dish{
		{
			ID:         1,
			Name:       "香辣鸡腿堡",
			Pic:        placeholderPic("Burger", "#d45d1f"),
			Detail:     "招牌单品，适合测试菜品列表和详情页。",
			Price:      18.9,
			Status:     1,
			CategoryID: 1,
			UpdateTime: nowText,
			Flavors: []flavor{
				{Name: "辣度", List: `["不辣","微辣","中辣"]`},
				{Name: "口味", List: `["原味","番茄","黑椒"]`},
			},
		},
		{
			ID:         2,
			Name:       "黑椒牛肉饭",
			Pic:        placeholderPic("Rice", "#8d5d44"),
			Detail:     "配图、价格、口味数据都可直接用于联调。",
			Price:      26.5,
			Status:     1,
			CategoryID: 1,
			UpdateTime: nowText,
			Flavors: []flavor{
				{Name: "温度", List: `["热饮","常温"]`},
			},
		},
		{
			ID:         3,
			Name:       "杨枝甘露",
			Pic:        placeholderPic("Drink", "#ef8f35"),
			Detail:     "饮品分类样例数据。",
			Price:      15.0,
			Status:     1,
			CategoryID: 2,
			UpdateTime: nowText,
			Flavors: []flavor{
				{Name: "甜味", List: `["无糖","少糖","半糖","全糖"]`},
				{Name: "温度", List: `["去冰","少冰","多冰"]`},
			},
		},
		{
			ID:         4,
			Name:       "脆薯拼盘",
			Pic:        placeholderPic("Snack", "#e9b44c"),
			Detail:     "停售菜品样例。",
			Price:      12.8,
			Status:     0,
			CategoryID: 1,
			UpdateTime: nowText,
		},
	}

	setmeals := []setmeal{
		{
			ID:         1,
			Name:       "双人快乐餐",
			Pic:        placeholderPic("Combo", "#c76a28"),
			Detail:     "包含汉堡、饮品和小食。",
			Price:      58.0,
			Status:     1,
			CategoryID: 3,
			UpdateTime: nowText,
			SetmealDishes: []setmealDish{
				{DishID: 1, Name: "香辣鸡腿堡", Price: 18.9, Copies: 2},
				{DishID: 3, Name: "杨枝甘露", Price: 15.0, Copies: 2},
			},
		},
		{
			ID:         2,
			Name:       "午间轻食套餐",
			Pic:        placeholderPic("Lunch", "#768f4e"),
			Detail:     "适合套餐新增/编辑页调试。",
			Price:      42.0,
			Status:     0,
			CategoryID: 4,
			UpdateTime: nowText,
			SetmealDishes: []setmealDish{
				{DishID: 2, Name: "黑椒牛肉饭", Price: 26.5, Copies: 1},
				{DishID: 3, Name: "杨枝甘露", Price: 15.0, Copies: 1},
			},
		},
	}

	employees := []employee{
		{
			ID:         1,
			Name:       "陈雨涵",
			Account:    "cyh",
			Password:   "123456",
			Phone:      "13800000001",
			Age:        25,
			Gender:     1,
			Pic:        placeholderPic("CYH", "#173239"),
			Status:     1,
			UpdateTime: nowText,
		},
		{
			ID:         2,
			Name:       "林晓",
			Account:    "linx",
			Password:   "123456",
			Phone:      "13800000002",
			Age:        23,
			Gender:     0,
			Pic:        placeholderPic("LX", "#2f8f83"),
			Status:     1,
			UpdateTime: nowText,
		},
		{
			ID:         3,
			Name:       "王策",
			Account:    "wangc",
			Password:   "123456",
			Phone:      "13800000003",
			Age:        29,
			Gender:     1,
			Pic:        placeholderPic("WC", "#d97706"),
			Status:     0,
			UpdateTime: nowText,
		},
	}

	orders := []orderVO{
		newOrder(1, "202606120001", 2, "张三", "13812340001", "上海市浦东新区世纪大道 100 号", 46.8, "少放辣椒", []orderDetail{
			{ID: 1, OrderID: 1, DishID: 1, Name: "香辣鸡腿堡", Number: 2, Amount: 18.9, Pic: dishes[0].Pic},
			{ID: 2, OrderID: 1, DishID: 3, Name: "杨枝甘露", Number: 1, Amount: 15.0, Pic: dishes[2].Pic},
		}),
		newOrder(2, "202606120002", 3, "李四", "13812340002", "上海市徐汇区漕溪北路 88 号", 58.0, "到店后电话联系", []orderDetail{
			{ID: 3, OrderID: 2, SetmealID: 1, Name: "双人快乐餐", Number: 1, Amount: 58.0, Pic: setmeals[0].Pic},
		}),
		newOrder(3, "202606120003", 4, "王五", "13812340003", "上海市静安区南京西路 66 号", 26.5, "无需餐具", []orderDetail{
			{ID: 4, OrderID: 3, DishID: 2, Name: "黑椒牛肉饭", Number: 1, Amount: 26.5, Pic: dishes[1].Pic},
		}),
		newOrder(4, "202606120004", 5, "赵六", "13812340004", "上海市杨浦区国权路 15 号", 42.0, "放门口即可", []orderDetail{
			{ID: 5, OrderID: 4, SetmealID: 2, Name: "午间轻食套餐", Number: 1, Amount: 42.0, Pic: setmeals[1].Pic},
		}),
		newOrder(5, "202606120005", 6, "孙七", "13812340005", "上海市闵行区七莘路 90 号", 18.9, "已取消", []orderDetail{
			{ID: 6, OrderID: 5, DishID: 1, Name: "香辣鸡腿堡", Number: 1, Amount: 18.9, Pic: dishes[0].Pic},
		}),
		newOrder(6, "202606120006", 1, "周八", "13812340006", "上海市长宁区天山路 120 号", 33.9, "等待支付", []orderDetail{
			{ID: 7, OrderID: 6, DishID: 4, Name: "脆薯拼盘", Number: 1, Amount: 12.8, Pic: dishes[3].Pic},
			{ID: 8, OrderID: 6, DishID: 3, Name: "杨枝甘露", Number: 1, Amount: 15.0, Pic: dishes[2].Pic},
		}),
	}

	return &mockStore{
		shopStatus:     1,
		nextEmployeeID: 3,
		nextCategoryID: 4,
		nextDishID:     4,
		nextSetmealID:  2,
		nextOrderID:    6,
		employees:      employees,
		categories:     categories,
		dishes:         dishes,
		setmeals:       setmeals,
		orders:         orders,
	}
}

func newOrder(id int, number string, status int, consignee, phone, address string, amount float64, remark string, details []orderDetail) orderVO {
	now := time.Now()
	orderTime := now.Add(-time.Duration(id) * time.Hour)
	estimated := orderTime.Add(45 * time.Minute)
	delivery := ""
	cancelTime := ""
	cancelReason := ""
	if status == 5 {
		delivery = orderTime.Add(35 * time.Minute).Format(timeLayout)
	}
	if status == 6 {
		cancelTime = orderTime.Add(15 * time.Minute).Format(timeLayout)
		cancelReason = "用户临时取消"
	}

	orderDishes := make([]string, 0, len(details))
	for _, item := range details {
		orderDishes = append(orderDishes, item.Name+"x"+strconv.Itoa(item.Number))
	}

	return orderVO{
		ID:                    id,
		Number:                number,
		Status:                status,
		UserID:                1000 + id,
		AddressBookID:         2000 + id,
		OrderTime:             orderTime.Format(timeLayout),
		CheckoutTime:          orderTime.Add(2 * time.Minute).Format(timeLayout),
		PayMethod:             1,
		PayStatus:             1,
		Amount:                amount,
		Remark:                remark,
		UserName:              consignee,
		Phone:                 phone,
		Address:               address,
		Consignee:             consignee,
		CancelReason:          cancelReason,
		CancelTime:            cancelTime,
		EstimatedDeliveryTime: estimated.Format(timeLayout),
		DeliveryStatus:        1,
		DeliveryTime:          delivery,
		PackAmount:            2.0,
		TablewareNumber:       2,
		TablewareStatus:       0,
		OrderDishes:           strings.Join(orderDishes, "、"),
		OrderDetailList:       details,
	}
}

func (s *mockStore) countOrdersByStatus(status int) int {
	total := 0
	for _, item := range s.orders {
		if item.Status == status {
			total++
		}
	}
	return total
}

func (s *mockStore) defaultDishCategoryID() int {
	for _, item := range s.categories {
		if item.Type == 1 {
			return item.ID
		}
	}
	return 1
}

func (s *mockStore) defaultSetmealCategoryID() int {
	for _, item := range s.categories {
		if item.Type == 2 {
			return item.ID
		}
	}
	return 3
}

func setCommonHeaders(w http.ResponseWriter) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
}

func decodeJSON(w http.ResponseWriter, r *http.Request, target interface{}) bool {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		writeJSON(w, http.StatusBadRequest, apiResponse{Code: 400, Msg: "invalid request body"})
		return false
	}
	if len(strings.TrimSpace(string(body))) == 0 {
		return true
	}
	if err := json.Unmarshal(body, target); err != nil {
		writeJSON(w, http.StatusBadRequest, apiResponse{Code: 400, Msg: "invalid json"})
		return false
	}
	return true
}

func writeSuccess(w http.ResponseWriter, data interface{}) {
	writeJSON(w, http.StatusOK, apiResponse{Code: 0, Msg: "success", Data: data})
}

func writeJSON(w http.ResponseWriter, status int, payload apiResponse) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(payload)
}

func parseID(w http.ResponseWriter, raw string) (int, bool) {
	id, err := strconv.Atoi(strings.TrimSpace(raw))
	if err != nil {
		writeJSON(w, http.StatusBadRequest, apiResponse{Code: 400, Msg: "invalid id"})
		return 0, false
	}
	return id, true
}

func parseIDs(raw string) []int {
	parts := strings.Split(raw, ",")
	result := make([]int, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if part == "" {
			continue
		}
		id, err := strconv.Atoi(part)
		if err != nil {
			continue
		}
		result = append(result, id)
	}
	return result
}

func queryPage(r *http.Request) (int, int) {
	return intQuery(r, "page", 1), intQuery(r, "pageSize", 10)
}

func intQuery(r *http.Request, key string, fallback int) int {
	value := strings.TrimSpace(r.URL.Query().Get(key))
	if value == "" {
		return fallback
	}
	number, err := strconv.Atoi(value)
	if err != nil {
		return fallback
	}
	return number
}

func paginate[T any](items []T, page int, pageSize int) pageResult[T] {
	if page <= 0 {
		page = 1
	}
	if pageSize <= 0 {
		pageSize = 10
	}
	total := len(items)
	start := (page - 1) * pageSize
	if start >= total {
		return pageResult[T]{Total: total, Records: []T{}}
	}
	end := start + pageSize
	if end > total {
		end = total
	}
	return pageResult[T]{
		Total:   total,
		Records: items[start:end],
	}
}

func placeholderPic(label, color string) string {
	svg := fmt.Sprintf(`<svg xmlns="http://www.w3.org/2000/svg" width="160" height="120" viewBox="0 0 160 120"><rect width="160" height="120" rx="18" fill="%s"/><text x="50%%" y="50%%" dominant-baseline="middle" text-anchor="middle" font-size="20" font-family="Arial" fill="white">%s</text></svg>`, color, label)
	return "data:image/svg+xml;utf8," + url.PathEscape(svg)
}

func nowString() string {
	return time.Now().Format(timeLayout)
}

func defaultString(value string, fallback string) string {
	if strings.TrimSpace(value) == "" {
		return fallback
	}
	return value
}

func intFromAny(value interface{}, fallback int) int {
	switch v := value.(type) {
	case nil:
		return fallback
	case int:
		return v
	case int32:
		return int(v)
	case int64:
		return int(v)
	case float64:
		return int(v)
	case float32:
		return int(v)
	case json.Number:
		number, err := v.Int64()
		if err == nil {
			return int(number)
		}
	case string:
		number, err := strconv.Atoi(strings.TrimSpace(v))
		if err == nil {
			return number
		}
	}
	return fallback
}

func floatFromAny(value interface{}, fallback float64) float64 {
	switch v := value.(type) {
	case nil:
		return fallback
	case float64:
		return v
	case float32:
		return float64(v)
	case int:
		return float64(v)
	case int32:
		return float64(v)
	case int64:
		return float64(v)
	case json.Number:
		number, err := v.Float64()
		if err == nil {
			return number
		}
	case string:
		number, err := strconv.ParseFloat(strings.TrimSpace(v), 64)
		if err == nil {
			return number
		}
	}
	return fallback
}

func randomPhone() string {
	return fmt.Sprintf("138%08d", rand.Intn(100000000))
}

func dateRange(r *http.Request) (time.Time, time.Time) {
	beginText := strings.TrimSpace(r.URL.Query().Get("begin"))
	endText := strings.TrimSpace(r.URL.Query().Get("end"))
	if beginText == "" || endText == "" {
		end := time.Now()
		begin := end.AddDate(0, 0, -6)
		return begin, end
	}

	begin, err1 := time.Parse("2006-01-02", beginText)
	end, err2 := time.Parse("2006-01-02", endText)
	if err1 != nil || err2 != nil || begin.After(end) {
		end := time.Now()
		begin := end.AddDate(0, 0, -6)
		return begin, end
	}
	return begin, end
}

func dateListBetween(begin, end time.Time) []string {
	result := make([]string, 0)
	for current := begin; !current.After(end); current = current.AddDate(0, 0, 1) {
		result = append(result, current.Format("2006-01-02"))
	}
	return result
}

func round2(value float64) float64 {
	number, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", value), 64)
	return number
}

func round4(value float64) float64 {
	number, _ := strconv.ParseFloat(fmt.Sprintf("%.4f", value), 64)
	return number
}
