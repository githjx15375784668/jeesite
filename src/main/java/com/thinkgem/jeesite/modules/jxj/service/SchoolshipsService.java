package com.thinkgem.jeesite.modules.jxj.service;
import java.util.HashMap;
import	java.util.Map;/**
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */


import java.util.Date;
import java.util.List;

import com.google.common.collect.Maps;
import com.thinkgem.jeesite.common.utils.IdGen;
import com.thinkgem.jeesite.modules.act.service.ActTaskService;
import com.thinkgem.jeesite.modules.jxj.util.ProcUtil;
import com.thinkgem.jeesite.modules.oa.entity.TestAudit;
import com.thinkgem.jeesite.modules.sys.entity.User;
import com.thinkgem.jeesite.modules.sys.utils.UserUtils;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.service.CrudService;
import com.thinkgem.jeesite.modules.jxj.entity.Schoolships;
import com.thinkgem.jeesite.modules.jxj.dao.SchoolshipsDao;

/**
 * 奖学金申请Service
 * @author 黄建新
 * @version 2019-11-21
 */
@Service
@Transactional(readOnly = true)
public class SchoolshipsService extends CrudService<SchoolshipsDao, Schoolships> {

	@Autowired
	private SchoolshipsDao schoolshipsDao;

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private ActTaskService actTaskService;
	public Schoolships get(String id) {
		return super.get(id);
	}
	
	public List<Schoolships> findList(Schoolships schoolships) {
		return super.findList(schoolships);
	}
	
	public Page<Schoolships> findPage(Page<Schoolships> page, Schoolships schoolships) {
		return super.findPage(page, schoolships);
	}
	
	@Transactional(readOnly = false)
	public void save(Schoolships schoolships) {
		//1、保存奖学金，需要一个奖学金的主键
		schoolships.setId(IdGen.uuid());
		User user = UserUtils.getUser();
		schoolships.setCreateBy(user);
		schoolships.setCreateDate(new Date());
		schoolships.setUpdateBy(user);
		schoolships.setUpdateDate(new Date());
		schoolshipsDao.insert(schoolships);
		//2、启动流程，同时获取流程实例id也就是我们快递里面的快递单号
		Map<String,Object> varMap=new HashMap<String,Object>();
		varMap.put("applyUser",user.getLoginName());
		varMap.put("title",user.getName()+"的奖学金申请");
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ProcUtil.SCHOLARSHIPS_PROCESS, ProcUtil.SCHOLARSHIPS_TABLE + ":" + schoolships.getId(), varMap);


		//3、流程实例Id跟奖学金对象关联
		schoolships.setProinsId(processInstance.getId());
		schoolshipsDao.update(schoolships);

	}
	
	@Transactional(readOnly = false)
	public void delete(Schoolships schoolships) {
		super.delete(schoolships);
	}



	@Transactional(readOnly = false)
	public void auditSave(Schoolships schoolships) {
			// 设置意见
		schoolships.getAct().setComment(("yes".equals(schoolships.getAct().getFlag())?"[同意] ":"[驳回] ")+schoolships.getAct().getComment());
		schoolships.preUpdate();
			// 对不同环节的业务逻辑进行操作
			String taskDefKey = schoolships.getAct().getTaskDefKey();

			// 提交流程任务
			Map<String, Object> vars = Maps.newHashMap();
			vars.put("pass", "yes".equals(schoolships.getAct().getFlag())?"1" : "0");
			actTaskService.complete(schoolships.getAct().getTaskId(), schoolships.getAct().getProcInsId(), schoolships.getAct().getComment(), vars);
		}
	}