package s24.budgetmanager.web;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import s24.budgetmanager.domain.Purchase;
import s24.budgetmanager.domain.PurchaseRepository;
import s24.budgetmanager.domain.SignupForm;
import s24.budgetmanager.domain.Budget;
import s24.budgetmanager.domain.BudgetRepository;
import s24.budgetmanager.domain.Category;
import s24.budgetmanager.domain.Categoryrepository;
import s24.budgetmanager.domain.Income;
import s24.budgetmanager.domain.IncomeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit; 


//Controller class for handling budget-related operations.

@Controller
public class BudgetController {

    @Autowired
    private PurchaseRepository repository;

    @Autowired
    private Categoryrepository crepository;

    @Autowired
    private BudgetRepository brepository;

    @Autowired
    private IncomeRepository irepository;

    // Login page request mapping
    @RequestMapping(value = "/login")
    public String login() {
        return "login";
    }

        // Mapping to display the purchase list
    @RequestMapping(value = { "/purchaselist" })
    public String purchaseList(Model model) {
    model.addAttribute("purchases", repository.findAll());
    // Fetch the latest budget from the repository
    Budget latestBudget = brepository.findFirstByOrderByBudgetidDesc(); 
    if (latestBudget != null) {
        long daysLeft = latestBudget.getDaysLeft();
        model.addAttribute("daysLeft", daysLeft);
        model.addAttribute("budget", latestBudget);
        model.addAttribute("dailyBudget", latestBudget.getDailyBudget());
    }
    return "purchaselist";
    }


    // Mapping to retrieve purchase list data as JSON
    @RequestMapping(value = "/purchases")
    public @ResponseBody List<Purchase> purchaseListRest() {
        return (List<Purchase>) repository.findAll();
    }

    // Mapping to find a specific purchase by ID
    @RequestMapping(value = "/purchase/{id}", method = RequestMethod.GET)
    public @ResponseBody Optional<Purchase> findPurchaseRest(@PathVariable("id") Long purchaseId) {
        return repository.findById(purchaseId);
    }

    // Mapping to add a new purchase
    @RequestMapping(value = "/add")
    public String addPurchase(Model model){
        Purchase purchase = new Purchase();
        purchase.setPurchaseDateTime(LocalDateTime.now()); 
        model.addAttribute("purchase", purchase);
        model.addAttribute("categories", crepository.findAll());
        return "addpurchase";
    } 

    // Mapping to add a new budget
    @RequestMapping(value = "/addbudget")
    public String addBudget(Model model){
        Budget budget = new Budget();
        model.addAttribute("budget", budget);
        return "addbudget";
    } 

     // Mapping to save a new budget
    @RequestMapping(value = "/savebudget", method = RequestMethod.POST)
    public String saveBudget(@RequestParam("name") String name,
                         @RequestParam("totalAmount") double totalAmount,
                         @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         Model model) {

    Budget existingBudget = brepository.findByName(name);
    if (existingBudget != null) {
        existingBudget.setTotalAmount(totalAmount);
        existingBudget.setEndDate(endDate);
    } else {
        existingBudget = new Budget(name, totalAmount ,startDate,endDate);
    }
    brepository.save(existingBudget);
    model.addAttribute("budget", existingBudget);
    return "redirect:/purchaselist";
    }

    // Mapping to add a new income
    @RequestMapping(value = "/addincome")
    public String addIncome(Model model){
        Income income = new Income();
        model.addAttribute("income", income);
        return "addincome";
    } 

     // Mapping to save a new income
     @RequestMapping(value = "/saveincome", method = RequestMethod.POST)
     public String saveIncome(@RequestParam("name") String name,
                          @RequestParam("month") int month,
                          @RequestParam("year") int year,
                          @RequestParam("amount") double totalAmount,
                          Model model) {
     Income existingIncome = irepository.findByName(name);
     if (existingIncome != null) {
        existingIncome.setMonth(month);
        existingIncome.setYear(year);
        existingIncome.setAmount(totalAmount);
     } else {
        existingIncome = new Income(name, month, year, totalAmount);
     }
     irepository.save(existingIncome);
        model.addAttribute("income", existingIncome);

        Budget latestBudget = brepository.findFirstByOrderByBudgetidDesc();
        if (latestBudget != null) {
            double updatedAmount = latestBudget.getTotalAmount() + totalAmount;
            latestBudget.setTotalAmount(updatedAmount);
            brepository.save(latestBudget);
        } else {
            // Handle case when no budget is found, possibly return an error or initialize a new budget
            model.addAttribute("error", "No budget found to update.");
            return "errorPage"; // Replace with your actual error page
        }

        return "redirect:/purchaselist";
    }

    // Mapping to save a purchase
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public String save(Purchase purchase) {
        Budget latestBudget = brepository.findFirstByOrderByBudgetidDesc();
        double updatedAmount = latestBudget.getTotalAmount() - purchase.getPrice();
        latestBudget.setTotalAmount(updatedAmount);
        brepository.save(latestBudget);
        repository.save(purchase);
        return "redirect:/purchaselist";
    }

    // Mapping to edit a purchase
    @RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
    public String editPurchase(@PathVariable("id") Long purchaseId, Model model) {
        Optional<Purchase> optionalPurchase = repository.findById(purchaseId);
        
        if (optionalPurchase.isPresent()) {
            Purchase purchase = optionalPurchase.get();
            model.addAttribute("purchase", purchase);
            model.addAttribute("categories", crepository.findAll());
            return "editpurchase";
        } else {
            return "error"; 
        }
    }

    // Mapping to delete a purchase
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public String deletePurchase(@PathVariable("id") Long purchaseId) {
        Purchase purchase = repository.findById(purchaseId).orElse(null);
        if (purchase != null) {
            Budget budget = purchase.getBudget();
            if (budget != null) {
                double remainingAmount = budget.getTotalAmount() - purchase.getPrice();
                budget.setTotalAmount(remainingAmount);
                brepository.save(budget);
            }
            repository.deleteById(purchaseId);
        }
        Budget latestBudget = brepository.findFirstByOrderByBudgetidDesc();
        double updatedAmount = latestBudget.getTotalAmount() + purchase.getPrice();
        latestBudget.setTotalAmount(updatedAmount);
        brepository.save(latestBudget);
        return "redirect:/purchaselist";
    }

    // Mapping to process signup form
    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String processSignUpForm(@ModelAttribute("signup") SignupForm signUp) {
        return "redirect:/purchaselist"; 
    }


    @GetMapping("/budgethistory")
    public String showBudgetHistory(Model model) {
        model.addAttribute("budgets", brepository.findAll());
        return "budgethistory";
    }

    @GetMapping("/incomehistory")
    public String showIncomeHistory(Model model) {
        model.addAttribute("incomes", irepository.findAll());
        return "incomehistory";
    }


}   