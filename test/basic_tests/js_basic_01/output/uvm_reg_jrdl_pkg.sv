//   Jrdl 160614.01 autogenerated file 
//   Date: Wed Jun 15 10:26:38 EDT 2016
//


// uvm_reg_rdl_pkg containing jrdl extended classes
`ifndef UVM_REG_JRDL_PKG_SV
  `define UVM_REG_JRDL_PKG_SV
  `include "uvm_macros.svh"
  package uvm_reg_jrdl_pkg;
    import uvm_pkg::*;
    
    typedef enum int unsigned {
      INTERRUPT = 64,
      DIAGNOSTIC = 128,
      CGATE_UNSAFE = 512,
      ERROR_COUNTER = 16,
      DYNAMIC_CONFIG = 2,
      STATIC_CONFIG = 1,
      CONSTRAINED_CONFIG = 4,
      STATE = 32,
      DEBUG = 256,
      STAT_COUNTER = 8
    } js_category_e;
    
    typedef enum int unsigned {
      MAJOR = 2,
      INFO = 1,
      FATAL = 4
    } js_subcategory_e;
    
    // uvm_reg_block_rdl class
    class uvm_reg_block_rdl extends uvm_reg_block;
      string m_rdl_tag;
      bit m_rdl_address_map = 0;
      string m_rdl_address_map_hdl_path = "";
      
      function new(string name = "uvm_reg_block_rdl", int has_coverage = UVM_NO_COVERAGE);
        super.new(name, has_coverage);
      endfunction: new
      
      function void set_rdl_address_map(bit val = 0);
        m_rdl_address_map = val;
      endfunction: set_rdl_address_map
      
      function void set_rdl_address_map_hdl_path(string path = "");
        m_rdl_address_map_hdl_path = path;
      endfunction: set_rdl_address_map_hdl_path
      
      function void set_rdl_tag(string rdl_tag = "rdl_tag");
        m_rdl_tag = rdl_tag;
      endfunction: set_rdl_tag
      
      function string get_rdl_name(string prefix, bit add_hdl_prefix = 0, string override_tag = "");
        uvm_reg_block_rdl rdl_parent;
        string rdl_tag;
        if (override_tag.len() > 0)
          rdl_tag = override_tag;
        else
          rdl_tag = m_rdl_tag;
        if (m_rdl_address_map) begin
          if (add_hdl_prefix)
            return {m_rdl_address_map_hdl_path, ".", prefix, rdl_tag};
          else
            return {prefix, rdl_tag};
        end
        if (get_parent() != null) begin
          $cast(rdl_parent, get_parent());
          return {rdl_parent.get_rdl_name(prefix, add_hdl_prefix, override_tag), rdl_tag};
        end
        return rdl_tag;
      endfunction: get_rdl_name
      
      virtual function void add_callbacks();
      endfunction: add_callbacks
      
      virtual function uvm_reg_block_rdl get_ancestor(int depth);
        uvm_reg_block_rdl rdl_parent;
        $cast(rdl_parent, get_parent());
        if (depth < 2) return rdl_parent;
        else return rdl_parent.get_ancestor(depth-1);
      endfunction: get_ancestor
      
      `uvm_object_utils(uvm_reg_block_rdl)
    endclass : uvm_reg_block_rdl
    
    // uvm_reg_rdl class
    class uvm_reg_rdl extends uvm_reg;
      local string m_rdl_tag;
      local bit m_external = 0;
      local bit m_dont_test = 0;
      local bit m_dont_compare = 0;
      local int unsigned m_js_category = 0;
      
      function new(string name = "uvm_reg_rdl", int unsigned n_bits = 0, int has_coverage = UVM_NO_COVERAGE);
        super.new(name, n_bits, has_coverage);
      endfunction: new
      
      function void set_rdl_tag(string rdl_tag = "rdl_tag");
        m_rdl_tag = rdl_tag;
      endfunction: set_rdl_tag
      
      function string get_rdl_name(string prefix, bit add_hdl_prefix = 0, string override_tag = "");
        uvm_reg_block_rdl rdl_parent;
        string rdl_tag;
        if (override_tag.len() > 0)
          rdl_tag = override_tag;
        else
          rdl_tag = m_rdl_tag;
        if (get_parent() != null) begin
          $cast(rdl_parent, get_parent());
          return {rdl_parent.get_rdl_name(prefix, add_hdl_prefix, override_tag), rdl_tag};
        end
        return rdl_tag;
      endfunction: get_rdl_name
      
      function void set_external(bit is_external);
        m_external = is_external;
      endfunction: set_external
      
      function bit is_external();
        return m_external;
      endfunction: is_external
      
      virtual function void get_intr_fields(ref uvm_reg_field fields[$]); // return all source interrupt fields
      endfunction: get_intr_fields
      
      virtual task get_active_intr_fields(ref uvm_reg_field fields[$], input bit is_halt, input uvm_path_e path = UVM_DEFAULT_PATH); // return all active source intr/halt fields
      endtask: get_active_intr_fields
      
      function void set_reg_test_info(bit dont_test, bit dont_compare, int js_category);
        m_dont_test = dont_test;
        m_dont_compare = dont_compare;
        m_js_category = js_category;
      endfunction: set_reg_test_info
      
      function bit is_dont_test();
        return m_dont_test;
      endfunction: is_dont_test
      
      function bit is_dont_compare();
        return m_dont_compare;
      endfunction: is_dont_compare
      
      function bit has_a_js_category();
        return (m_js_category > 0);
      endfunction: has_a_js_category
      
      function bit has_js_category(js_category_e cat);
        return ((cat & m_js_category) > 0);
      endfunction: has_js_category
      
      function void add_js_category(js_category_e cat);
        m_js_category = m_js_category | cat;
      endfunction: add_js_category
      
      function void remove_js_category(js_category_e cat);
        m_js_category = m_js_category & ~cat;
      endfunction: remove_js_category
      
      virtual function void add_callbacks();
      endfunction: add_callbacks
      
      virtual function uvm_reg_block_rdl get_ancestor(int depth);
        uvm_reg_block_rdl rdl_parent;
        $cast(rdl_parent, get_parent());
        if (depth < 2) return rdl_parent;
        else return rdl_parent.get_ancestor(depth-1);
      endfunction: get_ancestor
      
    endclass : uvm_reg_rdl
    
    // uvm_vreg_rdl class
    class uvm_vreg_rdl extends uvm_vreg;
      local bit m_dont_test = 0;
      local bit m_dont_compare = 0;
      local int unsigned m_js_category = 0;
      local bit m_has_reset = 0;
      local uvm_reg_data_t m_reset_value;
      local uvm_reg_data_t m_staged[longint unsigned];
      
      function new(string name = "uvm_vreg_rdl", int unsigned n_bits = 0);
        super.new(name, n_bits);
      endfunction: new
      
      function void set_reg_test_info(bit dont_test, bit dont_compare, int js_category);
        m_dont_test = dont_test;
        m_dont_compare = dont_compare;
        m_js_category = js_category;
      endfunction: set_reg_test_info
      
      function bit is_dont_test();
        return m_dont_test;
      endfunction: is_dont_test
      
      function bit is_dont_compare();
        return m_dont_compare;
      endfunction: is_dont_compare
      
      function bit has_a_js_category();
        return (m_js_category > 0);
      endfunction: has_a_js_category
      
      function bit has_js_category(js_category_e cat);
        return ((cat & m_js_category) > 0);
      endfunction: has_js_category
      
      function void add_js_category(js_category_e cat);
        m_js_category = m_js_category | cat;
      endfunction: add_js_category
      
      function void remove_js_category(js_category_e cat);
        m_js_category = m_js_category & ~cat;
      endfunction: remove_js_category
      
      function bit has_reset_value();
        return m_has_reset;
      endfunction: has_reset_value
      
      function uvm_reg_data_t get_reset_value();
        return m_reset_value;
      endfunction: get_reset_value
      
      function void set_reset_value(uvm_reg_data_t reset_value);
        m_has_reset = 1;
        m_reset_value = reset_value;
      endfunction: set_reset_value
      
      function uvm_reg_data_t get_staged(longint unsigned stage_idx); // return staged value at specified idx or reset value
        if (m_staged.exists(stage_idx)) return m_staged[stage_idx];
        else if (has_reset_value()) return m_reset_value;
        `uvm_error("RegModel", $sformatf("Accessing uninitialized staged value at index %d. (uvm_vreg_rdl::get_staged())", stage_idx));
        return 0;
      endfunction: get_staged
      
      function uvm_reg_data_t get_staged_field(longint unsigned stage_idx, string name); // return staged field value at specified idx
        uvm_reg_data_t rvalue;
        uvm_vreg_field vfld;
        int unsigned lsb;
        int unsigned fsize;
        vfld = this.get_field_by_name(name);
        if (vfld == null) begin;
          `uvm_error("RegModel", $sformatf("Unable to find field \"%s\" specified in uvm_vreg_rdl::get_staged_data()", name));
          return 0;
        end
        lsb = vfld.get_lsb_pos_in_register();
        fsize = vfld.get_n_bits();
        rvalue = get_staged(stage_idx);
        return (rvalue & (((1<<fsize)-1) << lsb)) >> lsb;
      endfunction: get_staged_field
      
      function void set_staged(longint unsigned stage_idx, uvm_reg_data_t staged); // set staged value at specified idx
        m_staged[stage_idx] = staged;
      endfunction: set_staged
      
      function void stage_field(longint unsigned stage_idx, string name, uvm_reg_data_t value); // set field value in specified staged array idx
        uvm_vreg_field vfld;
        int unsigned lsb;
        int unsigned fsize;
        vfld = this.get_field_by_name(name);
        if (vfld == null) begin;
          `uvm_error("RegModel", $sformatf("Unable to find field \"%s\" specified in uvm_vreg_rdl::stage_field()", name));
          return;
        end
        lsb = vfld.get_lsb_pos_in_register();
        fsize = vfld.get_n_bits();
        if (value >> fsize) begin
          `uvm_warning("RegModel", $sformatf("Staging value 'h%h that is greater than field \"%s\" size (%0d bits)", value, name, fsize));
          value &= ((1<<fsize)-1);
        end
        if (!m_staged.exists(stage_idx)) begin
          if (has_reset_value()) m_staged[stage_idx] = m_reset_value;
          else m_staged[stage_idx] = 0;
        end
        m_staged[stage_idx] |= (((1<<fsize)-1) << lsb);
        m_staged[stage_idx] ^= (((1<<fsize)-1) << lsb);
        m_staged[stage_idx] |= (value << lsb);
      endfunction: stage_field
      
      // write stage value stored at stage_idx to dut memory offset at vreg_idx
      virtual task write_staged(input longint unsigned stage_idx, input longint unsigned vreg_idx, output uvm_status_e status,
            input uvm_path_e path = UVM_DEFAULT_PATH, input uvm_reg_map map = null, input uvm_sequence_base parent = null,
            input uvm_object extension = null, input string fname = "", input int lineno = 0);
        if (!m_staged.exists(stage_idx)) begin
          `uvm_error("RegModel", $sformatf("Attempting write of uninitialized staged value at index %d. (uvm_vreg_rdl::write_staged())", stage_idx));
          return;
        end
        this.write(vreg_idx, status, m_staged[stage_idx], path, map, parent, extension, fname, lineno);
      endtask: write_staged
      
      // write stage value stored at idx to dut memory at same idx offset
      virtual task write_same_staged(input longint unsigned idx, output uvm_status_e status,
            input uvm_path_e path = UVM_DEFAULT_PATH, input uvm_reg_map map = null, input uvm_sequence_base parent = null,
            input uvm_object extension = null, input string fname = "", input int lineno = 0);
        this.write_staged(idx, idx, status, path, map, parent, extension, fname, lineno);
      endtask: write_same_staged
      
    endclass : uvm_vreg_rdl
    
    // uvm_mem_rdl class
    class uvm_mem_rdl extends uvm_mem;
      
      function new(string name = "uvm_mem_rdl", longint unsigned size = 1, int unsigned n_bits = 0, string  access = "RW", int has_coverage = UVM_NO_COVERAGE);
        super.new(name, size, n_bits, access, has_coverage);
      endfunction: new
      
    endclass : uvm_mem_rdl
    
    // derived rdl field class 
    class uvm_reg_field_rdl extends uvm_reg_field;
      protected bit m_is_counter = 0;
      protected bit m_is_interrupt = 0;
      protected bit m_is_dontcompare = 0;
      local bit m_is_sw_readable = 1;
      local bit m_is_sw_writeable = 1;
      local bit m_is_hw_readable = 1;
      local bit m_is_hw_writeable = 0;
      local bit m_has_hw_we = 0;
      local bit m_has_hw_wel = 0;
      local bit m_is_unsupported = 0;
      local int unsigned m_js_subcategory = 0;
      
      function new(string name = "uvm_reg_field_rdl");
        super.new(name);
      endfunction: new
      
      function uvm_reg_rdl get_rdl_register();
        uvm_reg_rdl rdl_reg;
        $cast(rdl_reg, get_register());
        return rdl_reg;
      endfunction: get_rdl_register
      
      function void set_rdl_access_info(bit is_sw_readable, bit is_sw_writeable, bit is_hw_readable, bit is_hw_writeable, bit has_hw_we, bit has_hw_wel);
        m_is_sw_readable = is_sw_readable;
        m_is_sw_writeable = is_sw_writeable;
        m_is_hw_readable = is_hw_readable;
        m_is_hw_writeable = is_hw_writeable;
        m_has_hw_we = has_hw_we;
        m_has_hw_wel = has_hw_wel;
      endfunction: set_rdl_access_info
      
      function string get_hw_read_signal();  // read data
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), this.get_name(), "_r"};
      endfunction: get_hw_read_signal
      
      function string get_hw_write_signal();  // write data
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_w"};
      endfunction: get_hw_write_signal
      
      function string get_hw_we_signal();  // write data enable
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_we"};
      endfunction: get_hw_we_signal
      
      function string get_hw_wel_signal();  // write data enable low
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_wel"};
      endfunction: get_hw_wel_signal
      
      function bit is_sw_readable();
        return m_is_sw_readable;
      endfunction: is_sw_readable
      
      function bit is_sw_writeable();
        return m_is_sw_writeable;
      endfunction: is_sw_writeable
      
      function bit is_hw_readable();
        return m_is_hw_readable;
      endfunction: is_hw_readable
      
      function bit is_hw_writeable();
        return m_is_hw_writeable;
      endfunction: is_hw_writeable
      
      function bit has_hw_we();
        return m_has_hw_we;
      endfunction: has_hw_we
      
      function bit has_hw_wel();
        return m_has_hw_wel;
      endfunction: has_hw_wel
      
      function bit is_counter();
        return m_is_counter;
      endfunction: is_counter
      
      function bit is_interrupt();
        return m_is_interrupt;
      endfunction: is_interrupt
      
      function void set_unsupported();
        m_is_unsupported = 1;
      endfunction: set_unsupported
      function bit is_unsupported();
        return m_is_unsupported;
      endfunction: is_unsupported
      
      function void set_dontcompare();
        m_is_dontcompare = 1;
      endfunction: set_dontcompare
      function bit is_dontcompare();
        return m_is_dontcompare;
      endfunction: is_dontcompare
      
      function bit has_a_js_subcategory();
        return (m_js_subcategory > 0);
      endfunction: has_a_js_subcategory
      
      function bit has_js_subcategory(js_subcategory_e cat);
        return ((cat & m_js_subcategory) > 0);
      endfunction: has_js_subcategory
      
      function void add_js_subcategory(js_subcategory_e cat);
        m_js_subcategory = m_js_subcategory | cat;
      endfunction: add_js_subcategory
      
      function void remove_js_subcategory(js_subcategory_e cat);
        m_js_subcategory = m_js_subcategory & ~cat;
      endfunction: remove_js_subcategory
      
      function void set_js_subcategory(int js_subcategory);
        m_js_subcategory = js_subcategory;
      endfunction: set_js_subcategory
      
    endclass : uvm_reg_field_rdl
    
    // counter field class 
    class uvm_reg_field_rdl_counter extends uvm_reg_field_rdl;
      local uvm_reg_data_t m_accum_value = 0;
      local string m_incr_sig;
      local uvm_reg_data_t m_incr_value = 1;
      local string m_incr_value_sig;
      local int unsigned m_incr_value_sig_width = 0;
      local bit m_has_incr_sat = 0;
      local uvm_reg_data_t m_incr_sat_value = 0;
      local string m_incr_sat_value_sig;
      local bit m_has_incr_thold = 0;
      local uvm_reg_data_t m_incr_thold_value = 0;
      local string m_incr_thold_value_sig;
      local string m_decr_sig;
      local uvm_reg_data_t m_decr_value = 1;
      local string m_decr_value_sig;
      local int unsigned m_decr_value_sig_width = 0;
      local bit m_has_decr_sat = 0;
      local uvm_reg_data_t m_decr_sat_value = 0;
      local string m_decr_sat_value_sig;
      local bit m_has_decr_thold = 0;
      local uvm_reg_data_t m_decr_thold_value = 0;
      local string m_decr_thold_value_sig;
      
      function new(string name = "uvm_reg_field_rdl_counter");
        super.new(name);
      endfunction: new
      
      function uvm_reg_data_t get_accum_value();
        return m_accum_value;
      endfunction: get_accum_value
      
      function void set_accum_value(uvm_reg_data_t accum_value);
        m_accum_value = accum_value;
      endfunction: set_accum_value
      
      function void add_incr(uvm_reg_data_t incr_value, string incr_sig = "", string incr_value_sig = "", int unsigned incr_value_sig_width = 0);
        m_is_counter = 1;
        m_incr_value = incr_value;
        if (incr_sig.len() > 0) m_incr_sig = incr_sig;
        if (incr_value_sig.len() > 0) m_incr_value_sig = incr_value_sig;
        m_incr_value_sig_width = incr_value_sig_width;
      endfunction: add_incr
      
      function void add_incr_sat(uvm_reg_data_t incr_sat_value, string incr_sat_value_sig = "");
        m_has_incr_sat = 1;
        m_incr_sat_value = incr_sat_value;
        if (incr_sat_value_sig.len() > 0) m_incr_sat_value_sig = incr_sat_value_sig;
      endfunction: add_incr_sat
      
      function void add_incr_thold(uvm_reg_data_t incr_thold_value, string incr_thold_value_sig = "");
        m_has_incr_thold = 1;
        m_incr_thold_value = incr_thold_value;
        if (incr_thold_value_sig.len() > 0) m_incr_thold_value_sig = incr_thold_value_sig;
      endfunction: add_incr_thold
      
      function string get_incr_signal();  // increment input
        string incr_signal;
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_incr_sig.len() > 0) incr_signal = {rdl_reg.get_rdl_name("rg_", 1), m_incr_sig};
        else incr_signal = {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_incr"};
        //$display("---  getting increment sigmal: %s", incr_signal);
        return incr_signal;
      endfunction: get_incr_signal
      
      function string get_overflow_signal();  // overflow output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), this.get_name(), "_overflow"};
      endfunction: get_overflow_signal
      
      function string get_incr_sat_signal();  // increment saturation output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), this.get_name(), "_incrsat_o"};
      endfunction: get_incr_sat_signal
      
      function string get_incr_thold_signal();  // increment threshold output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), this.get_name(), "_incrthold_o"};
      endfunction: get_incr_thold_signal
      
      function uvm_reg_data_t get_incr_value();
        return m_incr_value;
      endfunction: get_incr_value
      
      function string get_incr_value_signal();  // incr_value signal
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_incr_value_sig.len() > 0) return rdl_reg.get_rdl_name("rg_", 1, m_incr_value_sig);
        else return {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_incrvalue"};
      endfunction: get_incr_value_signal
      
      function int unsigned get_incr_value_signal_width();
        return m_incr_value_sig_width;
      endfunction: get_incr_value_signal_width
      
      function bit has_incr_sat();
        return m_has_incr_sat;
      endfunction: has_incr_sat
      function uvm_reg_data_t get_incr_sat_value();
        return m_incr_sat_value;
      endfunction: get_incr_sat_value
      
      function string get_incr_sat_value_signal();  // incr_sat_value signal
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_incr_sat_value_sig.len() < 1) return "";
        else return rdl_reg.get_rdl_name("rg_", 1, m_incr_sat_value_sig);
      endfunction: get_incr_sat_value_signal
      
      function bit has_incr_thold();
        return m_has_incr_thold;
      endfunction: has_incr_thold
      function uvm_reg_data_t get_incr_thold_value();
        return m_incr_thold_value;
      endfunction: get_incr_thold_value
      
      function string get_incr_thold_value_signal();  // incr_sat_value signal
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_incr_thold_value_sig.len() < 1) return "";
        else return rdl_reg.get_rdl_name("rg_", 1, m_incr_thold_value_sig);
      endfunction: get_incr_thold_value_signal
      
      function void add_decr(uvm_reg_data_t decr_value, string decr_sig = "", string decr_value_sig = "", int unsigned decr_value_sig_width = 0);
        m_is_counter = 1;
        m_decr_value = decr_value;
        if (decr_sig.len() > 0) m_decr_sig = decr_sig;
        if (decr_value_sig.len() > 0) m_decr_value_sig = decr_value_sig;
        m_decr_value_sig_width = decr_value_sig_width;
      endfunction: add_decr
      
      function void add_decr_sat(uvm_reg_data_t decr_sat_value, string decr_sat_value_sig = "");
        m_has_decr_sat = 1;
        m_decr_sat_value = decr_sat_value;
        if (decr_sat_value_sig.len() > 0) m_decr_sat_value_sig = decr_sat_value_sig;
      endfunction: add_decr_sat
      
      function void add_decr_thold(uvm_reg_data_t decr_thold_value, string decr_thold_value_sig = "");
        m_has_decr_thold = 1;
        m_decr_thold_value = decr_thold_value;
        if (decr_thold_value_sig.len() > 0) m_decr_thold_value_sig = decr_thold_value_sig;
      endfunction: add_decr_thold
      
      function string get_decr_signal();  // decrement input
        string decr_signal;
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_decr_sig.len() > 0) decr_signal = {rdl_reg.get_rdl_name("rg_", 1), m_decr_sig};
        else decr_signal = {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_decr"};
        //$display("---  getting decrement sigmal: %s", decr_signal);
        return decr_signal;
      endfunction: get_decr_signal
      
      function string get_underflow_signal();  // underflow output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), this.get_name(), "_underflow"};
      endfunction: get_underflow_signal
      
      function string get_decr_sat_signal();  // decrement saturation output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), this.get_name(), "_decrsat_o"};
      endfunction: get_decr_sat_signal
      
      function string get_decr_thold_signal();  // decrement threshold output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), this.get_name(), "_decrthold_o"};
      endfunction: get_decr_thold_signal
      
      function uvm_reg_data_t get_decr_value();
        return m_decr_value;
      endfunction: get_decr_value
      
      function string get_decr_value_signal();  // decr_value signal
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_decr_value_sig.len() > 0) return rdl_reg.get_rdl_name("rg_", 1, m_decr_value_sig);
        else return {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_decrvalue"};
      endfunction: get_decr_value_signal
      
      function int unsigned get_decr_value_signal_width();
        return m_decr_value_sig_width;
      endfunction: get_decr_value_signal_width
      
      function bit has_decr_sat();
        return m_has_decr_sat;
      endfunction: has_decr_sat
      function uvm_reg_data_t get_decr_sat_value();
        return m_decr_sat_value;
      endfunction: get_decr_sat_value
      
      function string get_decr_sat_value_signal();  // decr_sat_value signal
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_decr_sat_value_sig.len() < 1) return "";
        else return rdl_reg.get_rdl_name("rg_", 1, m_decr_sat_value_sig);
      endfunction: get_decr_sat_value_signal
      
      function bit has_decr_thold();
        return m_has_decr_thold;
      endfunction: has_decr_thold
      function uvm_reg_data_t get_decr_thold_value();
        return m_decr_thold_value;
      endfunction: get_decr_thold_value
      
      function string get_decr_thold_value_signal();  // decr_sat_value signal
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        if (m_decr_thold_value_sig.len() < 1) return "";
        else return rdl_reg.get_rdl_name("rg_", 1, m_decr_thold_value_sig);
      endfunction: get_decr_thold_value_signal
      
    endclass : uvm_reg_field_rdl_counter
    
    // interrupt field class 
    class uvm_reg_field_rdl_interrupt extends uvm_reg_field_rdl;
      local string m_intr_sig;
      local int m_intr_level_type = 0;
      local int m_intr_sticky_type = 0;
      local bit m_is_halt = 0;
      local bit m_mask_intr_bits = 0;
      local uvm_reg_field_rdl m_intr_mask_fld;
      local bit m_intr_mask_fld_is_enable;
      local uvm_reg_field_rdl m_halt_mask_fld;
      local bit m_halt_mask_fld_is_enable;
      local uvm_reg_rdl m_cascade_intr_reg;
      local bit m_cascade_reg_is_halt;
      
      function new(string name = "uvm_reg_field_rdl_interrupt");
        super.new(name);
      endfunction: new
      
      function void add_intr(int intr_level_type = 0, int intr_sticky_type = 0,
                  string intr_sig = "", bit mask_intr_bits = 0);
        m_is_interrupt = 1;
        if (intr_level_type > 0) m_intr_level_type = intr_level_type;
        if (intr_sticky_type > 0) m_intr_sticky_type = intr_sticky_type;
        if (intr_sig.len() > 0) m_intr_sig = intr_sig;
        m_mask_intr_bits = mask_intr_bits;
      endfunction: add_intr
      
      function string get_intr_signal();  // interrupt input
        uvm_reg_rdl rdl_reg;
        string intr_signal;
        rdl_reg = this.get_rdl_register();
        if (m_intr_sig.len() > 0) intr_signal = {rdl_reg.get_rdl_name("l2h_", 1), m_intr_sig};
        else intr_signal = {rdl_reg.get_rdl_name("h2l_", 1), this.get_name(), "_intr"};
        //$display("---  getting intrement sigmal: %s", intr_signal);
        return intr_signal;
      endfunction: get_intr_signal
      
      function string get_intr_out_signal();  // interrupt output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), "_intr_o"};
      endfunction: get_intr_out_signal
      
      function int get_intr_level_type();  // LEVEL(0), POSEDGE(1), NEGEDGE(2), BOTHEDGE(3)
        return m_intr_level_type;
      endfunction: get_intr_level_type
      
      function int get_intr_sticky_type();  // STICKYBIT(0), STICKY(1), NONSTICKY(2)
        return m_intr_sticky_type;
      endfunction: get_intr_sticky_type
      
      function bit get_mask_intr_bits();
        return m_mask_intr_bits;
      endfunction: get_mask_intr_bits
      
      function void add_halt();
        m_is_halt = 1;
      endfunction: add_halt
      
      function bit is_halt();
        return m_is_halt;
      endfunction: is_halt
      
      function string get_halt_out_signal();  // halt output
        uvm_reg_rdl rdl_reg;
        rdl_reg = this.get_rdl_register();
        return {rdl_reg.get_rdl_name("l2h_", 1), "_halt_o"};
      endfunction: get_halt_out_signal
      
      function void set_intr_mask_field(uvm_reg_field intr_mask_fld, bit intr_mask_fld_is_enable);
        $cast(m_intr_mask_fld, intr_mask_fld);
        m_intr_mask_fld_is_enable = intr_mask_fld_is_enable;
      endfunction: set_intr_mask_field
      
      function uvm_reg_field_rdl get_intr_mask_field();
        return m_intr_mask_fld;
      endfunction: get_intr_mask_field
      
      function bit has_intr_mask_field();
        return (m_intr_mask_fld != null);
      endfunction: has_intr_mask_field
      
      function bit intr_mask_field_is_enable();
        return m_intr_mask_fld_is_enable;
      endfunction: intr_mask_field_is_enable
      
      function uvm_reg_data_t get_intr_masked();
        uvm_reg_field_rdl mask_fld;
        if (has_intr_mask_field()) begin
          if (intr_mask_field_is_enable()) return get() & m_intr_mask_fld.get();
          else return get() & ~m_intr_mask_fld.get();
        end
        return get();
      endfunction: get_intr_masked
      
      function void set_halt_mask_field(uvm_reg_field halt_mask_fld, bit halt_mask_fld_is_enable);
        $cast(m_halt_mask_fld, halt_mask_fld);
        m_halt_mask_fld_is_enable = halt_mask_fld_is_enable;
      endfunction: set_halt_mask_field
      
      function uvm_reg_field_rdl get_halt_mask_field();
        return m_halt_mask_fld;
      endfunction: get_halt_mask_field
      
      function bit has_halt_mask_field();
        return (m_halt_mask_fld != null);
      endfunction: has_halt_mask_field
      
      function bit halt_mask_field_is_enable();
        return m_halt_mask_fld_is_enable;
      endfunction: halt_mask_field_is_enable
      
      function uvm_reg_data_t get_halt_masked();
        uvm_reg_field_rdl mask_fld;
        if (has_halt_mask_field()) begin
          if (halt_mask_field_is_enable()) return get() & m_halt_mask_fld.get();
          else return get() & ~m_halt_mask_fld.get();
        end
        return get();
      endfunction: get_halt_masked
      
      function void set_cascade_intr_reg(uvm_reg cascade_intr_reg, bit cascade_reg_is_halt);
        $cast(m_cascade_intr_reg, cascade_intr_reg);
        m_cascade_reg_is_halt = cascade_reg_is_halt;
      endfunction: set_cascade_intr_reg
      
      function uvm_reg_rdl get_cascade_intr_reg();
        return m_cascade_intr_reg;
      endfunction: get_cascade_intr_reg
      
      function bit has_cascade_intr_reg();
        return (m_cascade_intr_reg != null);
      endfunction: has_cascade_intr_reg
      
      function bit cascade_reg_is_halt();
        return m_cascade_reg_is_halt;
      endfunction: cascade_reg_is_halt
      
      function void get_intr_fields(ref uvm_reg_field fields[$]); // return all source interrupt fields
        if (has_cascade_intr_reg()) m_cascade_intr_reg.get_intr_fields(fields);
        else fields.push_back(this);
      endfunction: get_intr_fields
      
      task get_active_intr_fields(ref uvm_reg_field fields[$], input bit is_halt, input uvm_path_e path = UVM_DEFAULT_PATH); // return all active source intr/halt fields
        if (has_cascade_intr_reg()) m_cascade_intr_reg.get_active_intr_fields(fields, m_cascade_reg_is_halt, path);
        else if (is_halt && (|get_halt_masked())) fields.push_back(this);
        else if (!is_halt && (|get_intr_masked())) fields.push_back(this);
      endtask: get_active_intr_fields
      
    endclass : uvm_reg_field_rdl_interrupt
    
    // cbs class for alias register 
    class rdl_alias_reg_cbs extends uvm_reg_cbs;
      uvm_reg  m_alias_regs[$];
      
      function new(string name = "rdl_alias_reg_cbs");
        super.new(name);
      endfunction: new
      
      // set alias register group for this cbs
      function void set_alias_regs(uvm_reg alias_regs[$]);
        m_alias_regs = alias_regs;
      endfunction: set_alias_regs
      
      // set all regs in an alias group to same value post r/w
      task alias_group_predict(uvm_reg_item rw);
         uvm_reg_data_t 	updated_value;
        if (rw.status != UVM_IS_OK)
          return;
        if (rw.element_kind == UVM_REG) begin
          uvm_reg rg;
          $cast(rg, rw.element);
          if (m_alias_regs[0] != null) begin
            updated_value = rg.get();
            foreach (m_alias_regs[i]) begin
              void'(m_alias_regs[i].predict(updated_value));
              //$display("  new value for %s is %h", m_alias_regs[i].get_full_name(), m_alias_regs[i].get());
            end
          end
        end
      endtask
      
      // update all regs in group after read
      virtual task post_read(uvm_reg_item rw);
         //$display("*** post_read ***");
         alias_group_predict(rw);
      endtask
      
      // update all regs in group after write
      virtual task post_write(uvm_reg_item rw);
         //$display("*** post_write ***");
         alias_group_predict(rw);
      endtask
      
      `uvm_object_utils(rdl_alias_reg_cbs)
    endclass : rdl_alias_reg_cbs
    
    // cbs class for enabled/masked intr fields 
    class rdl_mask_intr_field_cbs extends uvm_reg_cbs;
      local uvm_reg_field_rdl_interrupt m_masked_field;
      
      function new(string name = "", uvm_reg_field masked_field = null);
        super.new(name);
        $cast(m_masked_field, masked_field);
      endfunction: new
      
      virtual function void post_predict(input uvm_reg_field fld, input uvm_reg_data_t previous, inout uvm_reg_data_t value, input uvm_predict_e kind, input uvm_path_e path, input uvm_reg_map map);
        if (kind == UVM_PREDICT_READ) begin
          value = m_masked_field.get_intr_masked();
        end
      endfunction: post_predict
      
      `uvm_object_utils(rdl_mask_intr_field_cbs)
    endclass : rdl_mask_intr_field_cbs
    
    // cbs class for fields with next or intr assigned as cascaded intr_o value
    class rdl_cascade_intr_field_cbs extends uvm_reg_cbs;
      local uvm_reg_field_rdl_interrupt m_cascade_field;
      
      function new(string name = "", uvm_reg_field cascade_field = null);
        super.new(name);
        $cast(m_cascade_field, cascade_field);
      endfunction: new
      
      virtual function void post_predict(input uvm_reg_field fld, input uvm_reg_data_t previous, inout uvm_reg_data_t value, input uvm_predict_e kind, input uvm_path_e path, input uvm_reg_map map);
        uvm_reg_field f[$];
        uvm_reg_field_rdl rdl_f;
        uvm_reg_field_rdl_interrupt rdl_intr_f;
        uvm_reg m_intr_o_reg;
        if (kind == UVM_PREDICT_READ) begin
          m_intr_o_reg = m_cascade_field.get_cascade_intr_reg();
          m_intr_o_reg.get_fields(f);
          value = 0;
          foreach(f[i]) begin
            $cast(rdl_f, f[i]);
            if (rdl_f.is_interrupt()) begin
              $cast(rdl_intr_f, rdl_f);
              if (rdl_intr_f.cascade_reg_is_halt) value = value | rdl_intr_f.get_halt_masked();
              else value = value | rdl_intr_f.get_intr_masked();
            end
          end
        end
      endfunction: post_predict
      
      `uvm_object_utils(rdl_cascade_intr_field_cbs)
    endclass : rdl_cascade_intr_field_cbs
    
  endpackage
`endif