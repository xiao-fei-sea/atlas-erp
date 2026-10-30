package com.erp.masterdata.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.entity.MdCustomerContact;
import com.erp.masterdata.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masterdata/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('masterdata:customer:view')")
    public Result<PageResult<MdCustomer>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String level,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(customerService.page(page, size, keyword, level, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('masterdata:customer:view')")
    public Result<List<Map<String, Object>>> options() {
        List<Map<String, Object>> data = customerService.listEnabled().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "code", c.getCode(), "name", c.getName()))
                .toList();
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:customer:view')")
    public Result<MdCustomer> detail(@PathVariable Long id) {
        return Result.ok(customerService.getRequired(id));
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('masterdata:customer:view')")
    public Result<List<MdCustomerContact>> contacts(@PathVariable Long id) {
        return Result.ok(customerService.listContacts(id));
    }

    @PostMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('masterdata:customer:update')")
    @OperLog(module = "基础资料", action = "新增客户联系人")
    public Result<Long> addContact(@PathVariable Long id, @RequestBody MdCustomerContact contact) {
        return Result.ok(customerService.addContact(id, contact));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAuthority('masterdata:customer:update')")
    @OperLog(module = "基础资料", action = "删除客户联系人")
    public Result<Void> deleteContact(@PathVariable Long id, @PathVariable Long contactId) {
        customerService.deleteContact(id, contactId);
        return Result.ok();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('masterdata:customer:create')")
    @OperLog(module = "基础资料", action = "新增客户")
    public Result<Long> create(@RequestBody MdCustomer customer) {
        return Result.ok(customerService.create(customer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:customer:update')")
    @OperLog(module = "基础资料", action = "修改客户")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdCustomer customer) {
        customerService.update(id, customer);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:customer:delete')")
    @OperLog(module = "基础资料", action = "删除客户")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }
}
