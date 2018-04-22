/**
 * 
 */
package org.ibase4j.web.sys;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.ibase4j.model.sys.SysUser;
import org.ibase4j.service.sys.SysAuthorizeService;
import org.ibase4j.service.sys.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import top.ibase4j.core.base.BaseController;
import top.ibase4j.core.support.Assert;
import top.ibase4j.core.support.HttpCode;
import top.ibase4j.core.util.SecurityUtil;
import top.ibase4j.core.util.UploadUtil;

/**
 * 用户管理控制器
 * 
 * @author ShenHuaJie
 * @version 2016年5月20日 下午3:12:12
 */
@RestController
@Api(value = "用户管理", description = "用户管理")
@RequestMapping(value = "/user")
public class SysUserController extends BaseController<SysUser, SysUserService> {
	@Autowired
	private SysAuthorizeService sysAuthorizeService;

	@PostMapping
	@ApiOperation(value = "修改用户信息")
	@RequiresPermissions("sys.base.user.update")
	public Object update(ModelMap modelMap, @RequestBody SysUser param) {
		Assert.isNotBlank(param.getAccount(), "ACCOUNT");
		Assert.length(param.getAccount(), 3, 15, "ACCOUNT");
		if (param.getId() != null) {
			SysUser user = ((SysUserService) service).queryById(param.getId());
			Assert.notNull(user, "USER", param.getId());
			if (StringUtils.isNotBlank(param.getPassword())) {
				if (!param.getPassword().equals(user.getPassword())) {
					param.setPassword(SecurityUtil.encryptPassword(param.getPassword()));
				}
			}
		}
		return super.update(modelMap, param);
	}

	// 查询用户
	@ApiOperation(value = "查询用户")
	@RequiresPermissions("sys.base.user.read")
	@PutMapping(value = "/read/list")
	public Object query(ModelMap modelMap, @RequestBody Map<String, Object> param) {
		return super.query(modelMap, param);
	}

	// 用户详细信息
	@ApiOperation(value = "用户详细信息")
	@RequiresPermissions("sys.base.user.read")
	@PutMapping(value = "/read/detail")
	public Object get(ModelMap modelMap, @RequestBody SysUser param) {
        SysUser sysUser = service.queryById(param.getId());
        sysUser.setPassword(null);
        return setSuccessModelMap(modelMap, sysUser);
	}

	// 用户详细信息
	@ApiOperation(value = "删除用户")
	@RequiresPermissions("sys.base.user.delete")
	@DeleteMapping
	public Object delete(ModelMap modelMap, @RequestBody SysUser param) {
		return super.delete(modelMap, param);
	}

	// 当前用户
	@ApiOperation(value = "当前用户信息")
	@GetMapping(value = "/read/promission")
	public Object promission(ModelMap modelMap) {
		Long id = getCurrUser();
        SysUser sysUser = service.queryById(id);
		sysUser.setPassword(null);
		modelMap.put("user", sysUser);
		List<?> menus = sysAuthorizeService.queryAuthorizeByUserId(id);
		modelMap.put("menus", menus);
		return setSuccessModelMap(modelMap);
	}

	// 当前用户
	@ApiOperation(value = "当前用户信息")
	@GetMapping(value = "/read/current")
	public Object current(ModelMap modelMap) {
		SysUser param = new SysUser();
		param.setId(getCurrUser());
		SysUser sysUser = service.queryById(param.getId());
        sysUser.setPassword(null);
        return setSuccessModelMap(modelMap, sysUser);
	}

	@ApiOperation(value = "修改个人信息")
	@PostMapping(value = "/update/person")
	public Object updatePerson(ModelMap modelMap, @RequestBody SysUser param) {
		param.setId(getCurrUser());
		param.setPassword(null);
		Assert.isNotBlank(param.getAccount(), "ACCOUNT");
		Assert.length(param.getAccount(), 3, 15, "ACCOUNT");
		return super.update(modelMap, param);
	}

	@ApiOperation(value = "修改用户头像")
	@PostMapping(value = "/update/avatar")
	public Object updateAvatar(HttpServletRequest request, ModelMap modelMap) {
		List<String> fileNames = UploadUtil.uploadImage(request, false);
		if (fileNames.size() > 0) {
			SysUser param = new SysUser();
			param.setId(getCurrUser());
			String filePath = UploadUtil.getUploadDir(request) + fileNames.get(0);
			// String avatar = UploadUtil.remove2DFS("sysUser", "user" +
			// sysUser.getId(), filePath).getRemotePath();
			// String avatar = UploadUtil.remove2Sftp(filePath, "user" +
			// sysUser.getId());
			param.setAvatar(filePath);
			return super.update(modelMap, param);
		} else {
			setModelMap(modelMap, HttpCode.BAD_REQUEST);
			modelMap.put("msg", "请选择要上传的文件！");
			return modelMap;
		}
	}

	// 修改密码
	@ApiOperation(value = "修改密码")
	@PostMapping(value = "/update/password")
	public Object updatePassword(ModelMap modelMap, @RequestBody SysUser param) {
		Assert.notNull(param.getId(), "USER_ID");
		Assert.isNotBlank(param.getOldPassword(), "OLDPASSWORD");
		Assert.isNotBlank(param.getPassword(), "PASSWORD");
		String encryptPassword = SecurityUtil.encryptPassword(param.getOldPassword());
		SysUser sysUser = ((SysUserService) service).queryById(param.getId());
		Assert.notNull(sysUser, "USER", param.getId());
		Long userId = getCurrUser();
		if (!param.getId().equals(userId)) {
			SysUser user = ((SysUserService) service).queryById(userId);
			if ("".equals(user.getUserType())) {
				throw new UnauthorizedException("您没有权限修改用户密码.");
			}
		} else {
			if (!sysUser.getPassword().equals(encryptPassword)) {
				throw new UnauthorizedException("原密码错误.");
			}
		}
		param.setPassword(encryptPassword);
		param.setUpdateBy(getCurrUser());
		return super.update(modelMap, param);
	}
}