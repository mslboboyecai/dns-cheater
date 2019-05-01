package cn.org.hentai.dns.controller;

import cn.org.hentai.dns.entity.Address;
import cn.org.hentai.dns.entity.Result;
import cn.org.hentai.dns.entity.Rule;
import cn.org.hentai.dns.service.AddressService;
import cn.org.hentai.dns.service.RuleService;
import cn.org.hentai.dns.util.IPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by matrixy on 2019/5/1.
 */
@Controller
@RequestMapping("/manage/rule")
public class RuleController extends BaseController
{
    @Autowired
    RuleService ruleService;

    @Autowired
    AddressService addrService;

    @RequestMapping("/")
    public String index()
    {
        return "/rule/index";
    }

    @RequestMapping("/json")
    @ResponseBody
    public Result listJson(@RequestParam(required = false) String name,
                           @RequestParam(defaultValue = "1") int pageIndex)
    {
        Result result = new Result();
        try
        {
            result.setData(ruleService.find(pageIndex, 20));
        }
        catch(Exception ex)
        {
            result.setError(ex);
        }
        return result;
    }

    // 添加新解析规则
    // ipFrom ipTo timeFrom timeTo matchMode name priority enabled dispatchMode
    @RequestMapping("/create")
    @ResponseBody
    @Transactional
    public Result createRule(@RequestParam String ipFrom,
                             @RequestParam String ipTo,
                             @RequestParam String timeFrom,
                             @RequestParam String timeTo,
                             @RequestParam String matchMode,
                             @RequestParam String name,
                             @RequestParam String dispatchMode,
                             @RequestParam String[] addr)
    {
        Result result = new Result();
        try
        {
            Rule rule = new Rule();
            if (!StringUtils.isEmpty(ipFrom))
            {
                if (ipFrom.matches("^(\\d{1,3})(\\.\\d{1,3}){3}$") == false)
                    throw new RuntimeException("请输入正确的IP开始地址");
                rule.setIpFrom(IPUtils.toInteger(ipFrom));
            }
            if (!StringUtils.isEmpty(ipTo))
            {
                if (ipTo.matches("^(\\d{1,3})(\\.\\d{1,3}){3}$") == false)
                    throw new RuntimeException("请输入正确的IP结束地址");
                rule.setIpTo(IPUtils.toInteger(ipTo));
            }

            if (!StringUtils.isEmpty(timeFrom))
            {
                if (timeFrom.matches("^\\d{2}:\\d{2}:\\d{2}$") == false) timeFrom = null;
                rule.setTimeFrom(Integer.parseInt(timeFrom.replace(":", "")));
            }
            if (!StringUtils.isEmpty(timeTo))
            {
                if (timeTo.matches("^\\d{2}:\\d{2}:\\d{2}$") == false) timeTo = null;
                rule.setTimeTo(Integer.parseInt(timeTo.replace(":", "")));
            }
            if (rule.getTimeFrom() == null || rule.getTimeTo() == null)
            {
                rule.setTimeFrom(null);
                rule.setTimeTo(null);
            }

            if (StringUtils.isEmpty(matchMode)) matchMode = "contains";
            if (!matchMode.matches("^(suffix)|(prefix)|(contains)$")) throw new RuntimeException("请选择匹配模式");
            if (StringUtils.isEmpty(name)) throw new RuntimeException("请输入要匹配解析的域名");
            if (StringUtils.isEmpty(dispatchMode)) dispatchMode = "round-robin";
            if (!dispatchMode.matches("^(round-robin)|(iphash)|(random)$")) throw new RuntimeException("请选择应答IP的分发模式");
            if (addr == null || addr.length == 0) throw new RuntimeException("请至少添加一个IP地址");

            rule.setPriority(0);
            rule.setMatchMode(matchMode);
            rule.setName(name);
            rule.setEnabled(true);
            rule.setDispatchMode(dispatchMode);

            ruleService.create(rule);

            for (int i = 0; i < addr.length; i++)
            {
                if (addr[i].matches("^(\\d{1,3})(\\.\\d{1,3}){3}$") == false) throw new RuntimeException("请输入正确格式的IP应答地址");

                Address item = new Address();
                item.setRuleId(rule.getId());
                item.setType("IPv4");
                item.setAddress(addr[i]);

                addrService.create(item);
            }

            // TODO: 实时更新内存缓存中的规则列表
        }
        catch(Exception ex)
        {
            result.setError(ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return result;
    }

    // 修改解析规则，含增删IP条目

    // 禁用/启用解析规则
}