package cn.edu.iip.nju.web;

import cn.edu.iip.nju.dao.InjureCaseDao;
import cn.edu.iip.nju.model.InjureCase;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * created by tanzhiwen
 * 2019/8/9
 */
@Controller
public class InjureCaseLabelController {
   @Autowired
   InjureCaseDao injureCaseDao;

    @GetMapping("/injurelabel")
    public String showinjureLabel(Model model) { return "injurelabel"; }

    @PostMapping("/injurelabel")
    public String getinjureLabel(@ModelAttribute InjureCase injure_label, @RequestParam String url , @RequestParam String pageUrl) {
        System.out.println(2);
        System.out.println(url);
        System.out.println(injure_label);
        List<InjureCase> injurecases = injureCaseDao.getAllByUrl(url);

        for (InjureCase injurecase : injurecases) {
            injurecase.setInjureType(injure_label.getInjureType());

            injurecase.setProductName(injure_label.getProductName());

            injurecase.setInjureArea(injure_label.getInjureArea());

            injurecase.setInjureDegree(injure_label.getInjureDegree());

            injurecase.setIsinjure(injure_label.getIsinjure());

            injurecase.setInjureTime(injure_label.getInjureTime());

            //injurecase.setProvince(injure_label.getProvince());

            System.out.println(injurecase.getInjureType());
            System.out.println(injurecase.getProductName());
            System.out.println(injurecase.getInjureArea());
            System.out.println(injurecase.getInjureDegree());
            System.out.println(injurecase.getIsinjure());
            System.out.println(injurecase.getInjureTime());
            System.out.println(injurecase.getProvince());
        }
        injureCaseDao.save(injurecases);
        String param = pageUrl.split("\\?")[1];
        System.out.println(injurecases);
        System.out.println(param);
        return "redirect:injurecase"+"?"+param;
    }

}
