export type UserInfo = {
  id: number
  account: string
  token: string
  /**
   * 角色：1=超级管理员，0=普通员工
   * 小白讲解：登录时后端返回，用来控制"新增员工/删除"等按钮的显隐
   */
  role: number
}